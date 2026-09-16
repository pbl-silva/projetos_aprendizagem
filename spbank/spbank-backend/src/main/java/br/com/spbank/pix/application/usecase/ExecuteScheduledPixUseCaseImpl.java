package br.com.spbank.pix.application.usecase;

import br.com.spbank.conta.application.exception.InsufficientFundsException;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.port.in.ExecuteScheduledPixUseCase;
import br.com.spbank.pix.application.port.out.BusinessLogPort;
import br.com.spbank.pix.application.port.out.PixBusinessEvent;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.service.PixProperties;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecuteScheduledPixUseCaseImpl
        implements ExecuteScheduledPixUseCase {

    private final PixPersistence pix;
    private final PixExecutionService execution;
    private final BusinessLogPort businessLogs;
    private final PixProperties properties;
    private final Clock clock;

    public ExecuteScheduledPixUseCaseImpl(
        PixPersistence pix,
        PixExecutionService execution,
        BusinessLogPort businessLogs,
        PixProperties properties,
        Clock clock
    ) {
        this.pix = pix;
        this.execution = execution;
        this.businessLogs = businessLogs;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PixPayment execute(UUID paymentId) {

        PixPayment payment = pix
            .findPaymentForUpdate(paymentId)
            .orElseThrow(() ->
                new NotFoundException(
                    "PIX_NOT_FOUND"
                )
            );

        requireDue(payment);

        payment.markProcessing();

        try {
            return execution.executeLocked(
                payment
            );

        } catch (InsufficientFundsException ex) {

            payment.markFailed(
                clock.instant(),
                "INSUFFICIENT_FUNDS_ON_EXECUTION_DATE",
                ex.getMessageKey()
            );

        } catch (BusinessException ex) {

            payment.markFailed(
                clock.instant(),
                ex.getCode(),
                ex.getMessageKey()
            );
        }

        PixPayment saved =
            pix.savePayment(payment);

        businessLogs.publish(
            new PixBusinessEvent(
                saved.getId(),
                saved.getStatus(),
                saved.getOriginAccountId(),
                saved.getAmount(),
                clock.instant(),
                saved.getFailureCode(),
                saved.getMaskedKey(),
                saved.getMaskedDocument(),
                saved.getKeyHash()
            )
        );

        return saved;
    }

    private void requireDue(
        PixPayment payment
    ) {
        if (!payment.isScheduled()) {
            throw new BusinessException(
                "PIX_NOT_SCHEDULED",
                "pix.not-scheduled"
            );
        }

        LocalDate scheduledFor =
            payment.getScheduledFor();

        if (scheduledFor == null) {
            throw new BusinessException(
                "PIX_NOT_SCHEDULED",
                "pix.not-scheduled"
            );
        }

        LocalDate today =
            LocalDate.now(
                clock.withZone(
                    ZoneId.of(
                        properties.zone()
                    )
                )
            );

        if (scheduledFor.isAfter(today)) {
            throw new BusinessException(
                "PIX_NOT_DUE",
                "pix.not-due"
            );
        }
    }
}