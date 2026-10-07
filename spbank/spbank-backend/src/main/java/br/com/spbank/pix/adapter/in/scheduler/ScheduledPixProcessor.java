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

    private final PixPersistence pix;
    private final ExecuteScheduledPixUseCase execute;
    private final Clock clock;
    private final PixProperties properties;

    public ScheduledPixProcessor(
        PixPersistence pix,
        ExecuteScheduledPixUseCase execute,
        Clock clock,
        PixProperties properties
    ) {
        this.pix = pix;
        this.execute = execute;
        this.clock = clock;
        this.properties = properties;
    }

    public void processDueBatch() {
        LocalDate today =
            clock
                .instant()
                .atZone(
                    ZoneId.of(
                        properties.zone()
                    )
                )
                .toLocalDate();

        pix.findDueIds(
            today,
            100
        ).forEach(
            id -> {
                try {
                    execute.execute(id);

                } catch (
                    RuntimeException ex
                ) {
                    LOG.error(
                        "Falha ao processar Pix agendado {}",
                        id,
                        ex
                    );
                }
            }
        );
    }
}