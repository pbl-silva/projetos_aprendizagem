package br.com.spbank.transferencia.application.usecase;

import br.com.spbank.conta.application.exception.AccountInactiveException;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.transferencia.application.model.Transfer;
import br.com.spbank.transferencia.application.model.TransferRecipient;
import br.com.spbank.transferencia.application.model.TransferType;
import br.com.spbank.transferencia.application.port.in.CreateTransferCommand;
import br.com.spbank.transferencia.application.port.in.CreateTransferUseCase;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;
import br.com.spbank.transferencia.application.service.BusinessCalendar;
import br.com.spbank.transferencia.application.service.TransferPolicy;
import br.com.spbank.transferencia.application.service.TransferRequestFingerprint;
import br.com.spbank.transferencia.application.service.TransferRoutingService;
import br.com.spbank.transferencia.application.serviceRoutingService.TransferRoute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTransferUseCaseImpl implements CreateTransferUseCase {

    private final AccountPersistence accounts;
    private final TransferPersistence transfers;
    private final TransferRoutingService routing;
    private final TransferPolicy policy;
    private final TransferExecutionService execution;
    private final BusinessCalendar calendar;
    private final Clock clock;

    public CreateTransferUseCaseImpl(
            AccountPersistence accounts,
            TransferPersistence transfers,
            TransferRoutingService routing,
            TransferPolicy policy,
            TransferExecutionService execution,
            BusinessCalendar calendar,
            Clock clock
    ) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.routing = routing;
        this.policy = policy;
        this.execution = execution;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Transfer create(CreateTransferCommand command) {

        String fingerprint =
                TransferRequestFingerprint.calculate(command);

        Optional<Transfer> previous =
                transfers.findBySourceAndIdempotencyKey(
                        command.sourceAccountId(),
                        command.idempotencyKey()
                );

        if (previous.isPresent()) {
            return sameRequest(
                    previous.get(),
                    fingerprint
            );
        }

        BigDecimal amount =
                money(command.amount());

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

        TransferType type =
                route.type();

        Optional<Account> target =
                route.targetAccount();

        if (target.isPresent()
                && source.getId()
                .equals(target.get().getId())) {

            throw new BusinessException(
                    "SAME_ACCOUNT",
                    "transfer.same-account"
            );
        }

        policy.requireWithinLimit(
                type,
                amount
        );

        List<UUID> accountIds;

        if (target.isPresent()) {

            accountIds = List.of(
                    source.getId(),
                    target.get().getId()
            );

        } else {

            accountIds = List.of(
                    source.getId()
            );
        }

        Map<UUID, Account> locked =
                accounts.findAllForUpdate(
                        accountIds
                );

        source = active(
                locked.get(
                        source.getId()
                )
        );

        if (target.isPresent()) {

            Account lockedTarget = active(
                    locked.get(
                            target.get().getId()
                    )
            );

            routing.validateRecipient(
                    lockedTarget,
                    command.recipientName(),
                    command.recipientDocument()
            );

            target = Optional.of(
                    lockedTarget
            );
        }

        Optional<Transfer> createdWhileWaiting =
                transfers.findBySourceAndIdempotencyKey(
                        command.sourceAccountId(),
                        command.idempotencyKey()
                );

        if (createdWhileWaiting.isPresent()) {

            return sameRequest(
                    createdWhileWaiting.get(),
                    fingerprint
            );
        }

        TransferRecipient recipient =
                new TransferRecipient(
                        command.recipientName(),

                        TransferRoutingService.digits(
                                command.recipientDocument()
                        ),

                        command.target().bankCode(),
                        command.target().branch(),
                        command.target().accountNumber(),
                        command.target().accountType()
                );

        UUID targetId = target
                .map(Account::getId)
                .orElse(null);

        var now =
                clock.instant();

        boolean mustSchedule =
                command.scheduledFor() != null
                        || (
                        type == TransferType.TED
                                && !calendar.isTedWindowOpen(now)
                );

        if (mustSchedule) {

            var effectiveDate =
                    type == TransferType.TED

                            ? calendar.effectiveTedDate(
                                    command.scheduledFor(),
                                    now
                            )

                            : command.scheduledFor();

            return transfers.save(
                    Transfer.scheduled(
                            type,
                            source.getId(),
                            targetId,
                            recipient,
                            amount,
                            BigDecimal.ZERO.setScale(2),
                            false,
                            command.idempotencyKey(),
                            fingerprint,
                            effectiveDate,
                            now
                    )
            );
        }

        Transfer transfer =
                transfers.save(
                        Transfer.processing(
                                type,
                                source.getId(),
                                targetId,
                                recipient,
                                amount,
                                BigDecimal.ZERO.setScale(2),
                                false,
                                command.idempotencyKey(),
                                fingerprint,
                                now
                        )
                );

        return execution.executeLocked(
                transfer
        );
    }

    private static Transfer sameRequest(
            Transfer previous,
            String fingerprint
    ) {

        if (!fingerprint.equals(
                previous.getRequestFingerprint()
        )) {

            throw new BusinessException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "transfer.idempotency-key-reused"
            );
        }

        return previous;
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

    private static BigDecimal money(
            BigDecimal value
    ) {

        return value.setScale(
                2,
                RoundingMode.UNNECESSARY
        );
    }
}