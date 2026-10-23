package br.com.spbank.pix.adapter.in.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.spbank.pix.application.port.in.ExecuteScheduledPixUseCase;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.service.PixProperties;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ScheduledPixProcessorTest {

    @Test
    void shouldContinueTheBatchWhenOnePixFailsTechnically() {
        PixPersistence persistence =
            java.util.Objects.requireNonNull(mock(PixPersistence.class));

        ExecuteScheduledPixUseCase execute =
            java.util.Objects.requireNonNull(mock(
                ExecuteScheduledPixUseCase.class
            ));

        Clock clock =
            Clock.fixed(
                Instant.parse(
                    "2026-08-27T13:00:00Z"
                ),
                ZoneOffset.UTC
            );

        PixProperties properties =
            new PixProperties(
                5000,
                new BigDecimal("50000.00"),
                new BigDecimal("1000.00"),
                "America/Sao_Paulo"
            );

        LocalDate today =
            LocalDate.of(
                2026,
                8,
                27
            );

        UUID first =
            UUID.fromString(
                "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"
            );

        UUID second =
            UUID.fromString(
                "ffffffff-ffff-ffff-ffff-ffffffffffff"
            );

        when(
            persistence.findDueIds(
                today,
                100
            )
        ).thenReturn(
            List.of(
                first,
                second
            )
        );

        doThrow(
            new RuntimeException(
                "database unavailable"
            )
        )
        .when(execute)
        .execute(first);

        new ScheduledPixProcessor(
            persistence,
            execute,
            clock,
            properties
        ).processDueBatch();

        InOrder order =
            inOrder(execute);

        order.verify(execute)
            .execute(first);

        order.verify(execute)
            .execute(second);
    }
}