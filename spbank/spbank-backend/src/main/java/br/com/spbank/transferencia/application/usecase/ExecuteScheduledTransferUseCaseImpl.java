package br.com.spbank.transferencia.application.usecase;

import br.com.spbank.conta.application.exception.InsufficientFundsException;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.transferencia.application.model.Transfer;
import br.com.spbank.transferencia.application.model.TransferType;
import br.com.spbank.transferencia.application.port.in.ExecuteScheduledTransferUseCase;
import br.com.spbank.transferencia.application.port.out.BusinessLogPort;
import br.com.spbank.transferencia.application.port.out.TransferBusinessEvent;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;
import br.com.spbank.transferencia.application.service.BusinessCalendar;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecuteScheduledTransferUseCaseImpl
        implements ExecuteScheduledTransferUseCase {

    private final TransferPersistence transfers;
    private final TransferExecutionService execution;
    private final BusinessLogPort businessLogs;
    private final BusinessCalendar calendar;
    private final Clock clock;

    public ExecuteScheduledTransferUseCaseImpl(
            TransferPersistence transfers,
            TransferExecutionService execution,
            BusinessLogPort businessLogs,
            BusinessCalendar calendar,
            Clock clock
    ) {
        this.transfers = transfers;
        this.execution = execution;
        this.businessLogs = businessLogs;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Transfer execute(UUID id) {

        Transfer transfer = transfers
                .findForUpdate(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "TRANSFER_NOT_FOUND"
                        )
                );

        var now = clock.instant();

        transfer.requireDue(
                calendar.today(now)
        );

        if (transfer.getType() == TransferType.TED
                && !calendar.isTedWindowOpen(now)) {

            return transfer;
        }

        transfer.startProcessing();

        try {

            return execution.executeLocked(
                    transfer
            );

        } catch (InsufficientFundsException ex) {

            transfer.fail(
                    "INSUFFICIENT_FUNDS_ON_EXECUTION_DATE",
                    ex.getMessageKey(),
                    clock.instant()
            );

        } catch (BusinessException ex) {

            transfer.fail(
                    ex.getCode(),
                    ex.getMessageKey(),
                    clock.instant()
            );
        }

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