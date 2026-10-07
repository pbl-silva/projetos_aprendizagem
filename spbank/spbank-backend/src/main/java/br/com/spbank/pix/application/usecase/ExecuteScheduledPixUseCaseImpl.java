package br.com.spbank.pix.application.usecase;

import br.com.spbank.conta.application.exception.InsufficientFundsException;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.port.in.ExecuteScheduledPixUseCase;
import br.com.spbank.pix.application.port.out.PixBusinessEvent;
import br.com.spbank.pix.application.port.out.PixBusinessLogPort;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.service.PixProperties;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;

import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecuteScheduledPixUseCaseImpl
        implements ExecuteScheduledPixUseCase {

    private final PixPersistence pix;
    private final PixExecutionService execution;
    private final PixBusinessLogPort logs;
    private final Clock clock;
    private final PixProperties properties;

    public ExecuteScheduledPixUseCaseImpl(
        PixPersistence pix,
        PixExecutionService execution,
        PixBusinessLogPort logs,
        Clock clock,
        PixProperties properties
    ) {
        this.pix = pix;
        this.execution = execution;
        this.logs = logs;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    @Transactional
    public PixPayment execute(
        UUID id
    ) {
        PixPayment payment =
            pix.findPaymentForUpdate(id)
                .orElseThrow(
                    () ->
                        new NotFoundException(
                            "PIX_NOT_FOUND"
                        )
                );

        payment.requireDue(
            clock
                .instant()
                .atZone(
                    ZoneId.of(
                        properties.zone()
                    )
                )
                .toLocalDate()
        );

        payment.startProcessing();

        try {
            return execution.executeLocked(
                payment
            );

        } catch (
            InsufficientFundsException ex
        ) {
            payment.fail(
                "INSUFFICIENT_FUNDS_ON_EXECUTION_DATE",
                ex.getMessageKey(),
                clock.instant()
            );

        } catch (
            BusinessException ex
        ) {
            payment.fail(
                ex.getCode(),
                ex.getMessageKey(),
                clock.instant()
            );
        }

        PixPayment saved =
            pix.savePayment(
                payment
            );

        logs.publish(
            new PixBusinessEvent(
                saved.getId(),
                saved.getStatus(),
                saved.getSourceAccountId(),
                saved.getAmount(),
                saved.getKeyType(),
                saved.getKeyHash(),
                clock.instant(),
                saved.getFailureCode()
            )
        );

        return saved;
    }
}