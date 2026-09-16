package br.com.spbank.pix.adapter.in.scheduler;

import br.com.spbank.pix.application.port.in.ExecuteScheduledPixUseCase;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.service.PixProperties;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class ScheduledPixProcessor {

    private static final Logger LOG =
        LoggerFactory.getLogger(
            ScheduledPixProcessor.class
        );

    private static final int BATCH_SIZE = 100;

    private final PixPersistence pix;
    private final ExecuteScheduledPixUseCase execute;
    private final PixProperties properties;
    private final Clock clock;

    public ScheduledPixProcessor(
        PixPersistence pix,
        ExecuteScheduledPixUseCase execute,
        PixProperties properties,
        Clock clock
    ) {
        this.pix = pix;
        this.execute = execute;
        this.properties = properties;
        this.clock = clock;
    }

    public void processDueBatch() {

        LocalDate today =
            LocalDate.now(
                clock.withZone(
                    ZoneId.of(
                        properties.zone()
                    )
                )
            );

        pix.findDuePaymentIds(
            today,
            BATCH_SIZE
        ).forEach(paymentId -> {

            try {
                execute.execute(paymentId);

            } catch (RuntimeException ex) {

                LOG.error(
                    "Falha ao processar PIX agendado {}",
                    paymentId,
                    ex
                );
            }
        });
    }
}