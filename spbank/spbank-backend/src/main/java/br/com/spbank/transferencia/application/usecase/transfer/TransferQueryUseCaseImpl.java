package br.com.spbank.transferencia.application.usecase.transfer;

import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.transferencia.application.model.transfer.Transfer;
import br.com.spbank.transferencia.application.port.in.GetTransferUseCase;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransferQueryUseCaseImpl
        implements GetTransferUseCase {

    private final TransferPersistence transfers;

    public TransferQueryUseCaseImpl(
            TransferPersistence transfers
    ) {
        this.transfers = transfers;
    }

    @Override
    public Transfer get(
            UUID id,
            UUID sourceId
    ) {

        Transfer transfer = transfers
                .findById(id)
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

        return transfer;
    }
}