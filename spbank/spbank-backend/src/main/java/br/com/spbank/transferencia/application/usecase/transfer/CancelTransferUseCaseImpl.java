package br.com.spbank.transferencia.application.usecase.transfer;

import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.transferencia.application.model.transfer.Transfer;
import br.com.spbank.transferencia.application.port.in.CancelTransferUseCase;
import br.com.spbank.transferencia.application.port.out.BusinessLogPort;
import br.com.spbank.transferencia.application.port.out.TransferBusinessEvent;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelTransferUseCaseImpl
        implements CancelTransferUseCase {

    private final TransferPersistence transfers;
    private final BusinessLogPort businessLogs;
    private final Clock clock;

    public CancelTransferUseCaseImpl(
            TransferPersistence transfers,
            BusinessLogPort businessLogs,
            Clock clock
    ) {
        this.transfers = transfers;
        this.businessLogs = businessLogs;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Transfer cancel(
            UUID id,
            UUID sourceId
    ) {

        Transfer transfer = transfers
                .findForUpdate(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "TRANSFER_NOT_FOUND"
                        )
                );

        if (!transfer.getSourceAccountId()
                .equals(sourceId)) {

            throw new BusinessException(
                    "TRANSFER_ACCESS_DENIED",
                    "transfer.access-denied"
            );
        }

        transfer.cancel(
                clock.instant()
        );

        Transfer saved =
                transfers.save(transfer);

        businessLogs.publish(
                new TransferBusinessEvent(
                        saved.getId(),
                        saved.getStatus(),
                        saved.getSourceAccountId(),
                        saved.getAmount(),
                        saved.getFee(),
                        clock.instant(),
                        saved.getFailureCode()
                )
        );

        return saved;
    }
}