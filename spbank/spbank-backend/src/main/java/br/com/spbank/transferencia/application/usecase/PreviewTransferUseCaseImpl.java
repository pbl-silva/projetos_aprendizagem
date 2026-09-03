package br.com.spbank.transferencia.application.usecase;

import br.com.spbank.conta.application.exception.AccountInactiveException;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.transferencia.application.model.TransferType;
import br.com.spbank.transferencia.application.port.in.PreviewTransferCommand;
import br.com.spbank.transferencia.application.port.in.PreviewTransferUseCase;
import br.com.spbank.transferencia.application.port.in.TransferPreview;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;
import br.com.spbank.transferencia.application.service.BusinessCalendar;
import br.com.spbank.transferencia.application.service.TedFeePolicy;
import br.com.spbank.transferencia.application.service.TransferPolicy;
import br.com.spbank.transferencia.application.service.TransferRoutingService;
import br.com.spbank.transferencia.application.service.TransferRoutingService.TransferRoute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PreviewTransferUseCaseImpl
        implements PreviewTransferUseCase {

    private final AccountPersistence accounts;
    private final TransferPersistence transfers;
    private final TransferRoutingService routing;
    private final TransferPolicy policy;
    private final TedFeePolicy fees;
    private final BusinessCalendar calendar;
    private final Clock clock;

    public PreviewTransferUseCaseImpl(
            AccountPersistence accounts,
            TransferPersistence transfers,
            TransferRoutingService routing,
            TransferPolicy policy,
            TedFeePolicy fees,
            BusinessCalendar calendar,
            Clock clock
    ) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.routing = routing;
        this.policy = policy;
        this.fees = fees;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Override
    public TransferPreview preview(
            PreviewTransferCommand command
    ) {

        BigDecimal amount = command.amount()
                .setScale(
                        2,
                        RoundingMode.UNNECESSARY
                );

        Account source = active(
                accounts.findById(
                        command.sourceAccountId()
                ).orElseThrow(() ->
                        new NotFoundException(
                                "SOURCE_ACCOUNT_NOT_FOUND"
                        )
                )
        );

        TransferRoute route = routing.resolve(
                command.target(),
                command.recipientName(),
                command.recipientDocument()
        );

        if (route.targetAccount().isPresent()
                && source.getId().equals(
                        route.targetAccount()
                                .get()
                                .getId()
                )) {

            throw new BusinessException(
                    "SAME_ACCOUNT",
                    "transfer.same-account"
            );
        }

        policy.requireWithinLimit(
                route.type(),
                amount
        );

        var now = clock.instant();

        long completedTeds =
                route.type() == TransferType.TED

                        ? transfers.countCompletedBySourceAndTypeBetween(
                                source.getId(),
                                TransferType.TED,
                                calendar.monthStart(now),
                                calendar.nextMonthStart(now)
                        )

                        : 0;

        BigDecimal fee =
                route.type() == TransferType.TED

                        ? fees.feeFor(
                                source,
                                completedTeds
                        )

                        : BigDecimal.ZERO.setScale(2);

        int remainingFreeTeds =
                route.type() == TransferType.TED

                        ? fees.remainingFreeTeds(
                                source,
                                completedTeds
                        )

                        : 0;

        LocalDate effectiveDate =
                route.type() == TransferType.TED

                        ? calendar.effectiveTedDate(
                                command.scheduledFor(),
                                now
                        )

                        : command.scheduledFor() == null
                                ? calendar.today(now)
                                : command.scheduledFor();

        return new TransferPreview(
                route.type(),
                amount,
                fee,
                amount.add(fee),
                effectiveDate,
                route.institution().name(),
                remainingFreeTeds
        );
    }

    private static Account active(
            Account account
    ) {

        if (account == null) {

            throw new BusinessException(
                    "ACCOUNT_UNAVAILABLE",
                    "account.unavailable"
            );
        }

        if (!account.isActive()) {
            throw new AccountInactiveException();
        }

        return account;
    }
}