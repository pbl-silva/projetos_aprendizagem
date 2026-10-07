package br.com.spbank.pix.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.spbank.conta.application.exception.InsufficientFundsException;
import br.com.spbank.pix.application.model.PixDestination;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.model.PixStatus;
import br.com.spbank.pix.application.port.out.PixBusinessLogPort;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.service.PixProperties;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ExecuteScheduledPixUseCaseImplTest {

    @Test
    void shouldMarkDuePixAsFailedWhenBalanceIsInsufficientAtExecution() {
        Instant now =
            Instant.parse(
                "2026-08-27T13:00:00Z"
            );

        UUID source =
            UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
            );

        PixPayment payment =
            PixPayment.scheduled(
                source,
                PixDestination.internal(
                    UUID.randomUUID(),
                    "Roberta Silva",
                    "***7735",
                    "90001"
                ),
                PixKeyType.EMAIL,
                "r***@spbank.local",
                "a".repeat(64),
                new BigDecimal("75.00"),
                UUID.randomUUID(),
                "b".repeat(64),
                now.minusSeconds(60),
                LocalDate.of(
                    2026,
                    8,
                    27
                ),
                null,
                null,
                null,
                null
            );

        PixPersistence persistence =
            mock(PixPersistence.class);

        PixExecutionService execution =
            mock(PixExecutionService.class);

        PixBusinessLogPort logs =
            mock(PixBusinessLogPort.class);

        when(
            persistence.findPaymentForUpdate(
                payment.getId()
            )
        ).thenReturn(
            Optional.of(payment)
        );

        when(
            execution.executeLocked(
                payment
            )
        ).thenThrow(
            new InsufficientFundsException()
        );

        when(
            persistence.savePayment(
                payment
            )
        ).thenReturn(
            payment
        );

        var service =
            new ExecuteScheduledPixUseCaseImpl(
                persistence,
                execution,
                logs,
                Clock.fixed(
                    now,
                    ZoneOffset.UTC
                ),
                new PixProperties(
                    5000,
                    new BigDecimal("50000.00"),
                    new BigDecimal("1000.00"),
                    "America/Sao_Paulo"
                )
            );

        PixPayment result =
            service.execute(
                payment.getId()
            );

        assertThat(
            result.getStatus()
        ).isEqualTo(
            PixStatus.FAILED
        );

        assertThat(
            result.getFailureCode()
        ).isEqualTo(
            "INSUFFICIENT_FUNDS_ON_EXECUTION_DATE"
        );

        verify(persistence)
            .savePayment(payment);

        verify(logs)
            .publish(
                argThat(
                    event ->
                        event.status()
                            == PixStatus.FAILED
                        && "INSUFFICIENT_FUNDS_ON_EXECUTION_DATE"
                            .equals(
                                event.failureCode()
                            )
                )
            );
    }
}