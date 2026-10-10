
package br.com.spbank.transferencia.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class ScheduledTransferJob {

    private final ScheduledTransferProcessor processor;

    public ScheduledTransferJob(
            ScheduledTransferProcessor processor
    ) {
        this.processor = processor;
    }

    @Scheduled(
            fixedDelayString = "${spbank.transfer.scheduler-delay}"
    )
    public void process() {
        processor.processDueBatch();
    }
}
