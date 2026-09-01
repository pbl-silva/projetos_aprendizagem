package br.com.spbank.transferencia.application.usecase;

import br.com.spbank.transferencia.application.model;
import br.com.spbank.transferencia.application.modelStatus;
import br.com.spbank.transferencia.application.port.in.ListScheduledTransfersUseCase;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListScheduledTransfersUseCaseImpl
        implements ListScheduledTransfersUseCase {

    private final TransferPersistence transfers;

    public ListScheduledTransfersUseCaseImpl(
            TransferPersistence transfers
    ) {
        this.transfers = transfers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transfer> list(
            UUID sourceAccountId
    ) {

        return transfers.findBySourceAndStatus(
                sourceAccountId,
                TransferStatus.SCHEDULED,
                50
        );
    }
}