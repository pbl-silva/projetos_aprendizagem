package br.com.spbank.pix.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class ScheduledPixJob {

    private final ScheduledPixProcessor processor;

    public ScheduledPixJob(
        ScheduledPixProcessor processor
    ) {
        this.processor = processor;
    }

    @Scheduled(
        fixedDelayString =
            "${spbank.pix.scheduler-delay}"
    )
    public void process() {
        processor.processDueBatch();
    }
}