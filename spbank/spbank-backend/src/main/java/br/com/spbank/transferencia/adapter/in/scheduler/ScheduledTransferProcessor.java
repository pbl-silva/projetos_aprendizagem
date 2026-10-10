
package br.com.spbank.transferencia.adapter.in.scheduler;

import br.com.spbank.transferencia.application.model.TransferStatus;
import br.com.spbank.transferencia.application.port.in.ExecuteScheduledTransferUseCase;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;
import br.com.spbank.transferencia.application.service.BusinessCalendar;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public final class ScheduledTransferProcessor {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    ScheduledTransferProcessor.class
            );

    private final TransferPersistence transfers;
    private final ExecuteScheduledTransferUseCase execute;
    private final BusinessCalendar calendar;
    private final Clock clock;

    public ScheduledTransferProcessor(
            TransferPersistence transfers,
            ExecuteScheduledTransferUseCase execute,
            BusinessCalendar calendar,
            Clock clock
    ) {
        this.transfers = transfers;
        this.execute = execute;
        this.calendar = calendar;
        this.clock = clock;
    }

    public void processDueBatch() {

        LocalDate today =
                calendar.today(
                        clock.instant()
                );

        transfers.findDueIds(
                TransferStatus.SCHEDULED,
                today,
                100
        ).forEach(
                id -> {
                    try {
                        execute.execute(id);

                    } catch (RuntimeException ex) {
                        LOG.error(
                                "Falha ao processar agendamento {}",
                                id,
                                ex
                        );
                    }
                }
        );
    }
}
