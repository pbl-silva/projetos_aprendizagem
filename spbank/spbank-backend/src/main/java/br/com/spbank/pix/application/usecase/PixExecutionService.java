package br.com.spbank.pix.application.usecase;

import br.com.spbank.conta.application.exception.AccountInactiveException;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountEntry;
import br.com.spbank.conta.application.model.EntryDirection;
import br.com.spbank.conta.application.model.EntryReferenceType;
import br.com.spbank.conta.application.model.EntryType;
import br.com.spbank.conta.application.port.out.AccountEntryPersistence;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.pix.application.model.PixDestinationScope;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.port.out.BusinessLogPort;
import br.com.spbank.pix.application.port.out.PixBusinessEvent;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.port.out.PixSettlementPort;
import br.com.spbank.pix.application.service.PixProperties;
import br.com.spbank.shared.application.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PixExecutionService {

    private final AccountPersistence accounts;
    private final PixPersistence pix;
    private final AccountEntryPersistence entries;
    private final BusinessLogPort businessLogs;
    private final PixSettlementPort settlement;
    private final PixProperties properties;
    private final Clock clock;

    public PixExecutionService(
        AccountPersistence accounts,
        PixPersistence pix,
        AccountEntryPersistence entries,
        BusinessLogPort businessLogs,
        PixSettlementPort settlement,
        PixProperties properties,
        Clock clock
    ) {
        this.accounts = accounts;
        this.pix = pix;
        this.entries = entries;
        this.businessLogs = businessLogs;
        this.settlement = settlement;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(
        propagation = Propagation.MANDATORY,
        noRollbackFor = BusinessException.class
    )
    public PixPayment executeLocked(
        PixPayment payment
    ) {
        List<UUID> accountIds =
            accountIds(payment);

        Map<UUID, Account> locked =
            accounts.findAllForUpdate(
                accountIds
            );

        Account source = active(
            locked.get(
                payment.getOriginAccountId()
            )
        );

        Optional<Account> target =
            payment.getDestinationScope()
                == PixDestinationScope.INTERNAL

                ? Optional.of(
                    active(
                        locked.get(
                            requireDestinationAccountId(
                                payment
                            )
                        )
                    )
                )

                : Optional.empty();

        if (target.isPresent()
            && source.getId().equals(
                target.orElseThrow().getId()
            )) {

            throw new BusinessException(
                "PIX_SAME_ACCOUNT",
                "pix.same-account"
            );
        }

        BigDecimal amount =
            payment.getAmount();

        requireWithinLimit(amount);

        source.requireAvailable(amount);

        Instant now =
            clock.instant();

        List<AccountEntry> batch =
            new ArrayList<>();

        source.debit(amount);

        batch.add(
            AccountEntry.create(
                source,
                payment.getId(),
                EntryReferenceType.PIX,
                EntryType.PIX_OUT,
                EntryDirection.DEBIT,
                amount,
                "Pix enviado para "
                    + payment.getHolderName(),
                now,
                payment.getHolderName(),
                payment.getBankCode(),
                "PIX"
            )
        );

        String settlementReference = null;

        if (target.isPresent()) {
            Account internalTarget =
                target.orElseThrow();

            internalTarget.credit(amount);

            batch.add(
                AccountEntry.create(
                    internalTarget,
                    payment.getId(),
                    EntryReferenceType.PIX,
                    EntryType.PIX_IN,
                    EntryDirection.CREDIT,
                    amount,
                    "Pix recebido de "
                        + source.getHolderName(),
                    now,
                    source.getHolderName(),
                    source.getBankCode(),
                    "PIX"
                )
            );

        } else {
            settlementReference =
                settlement.settle(payment);
        }

        payment.markCompleted(
            now,
            settlementReference
        );

        List<Account> changedAccounts =
            new ArrayList<>();

        changedAccounts.add(source);

        target.ifPresent(
            changedAccounts::add
        );

        accounts.saveAll(
            changedAccounts
        );

        entries.saveAll(
            batch
        );

        PixPayment saved =
            pix.savePayment(payment);

        businessLogs.publish(
            new PixBusinessEvent(
                saved.getId(),
                saved.getStatus(),
                saved.getOriginAccountId(),
                saved.getAmount(),
                now,
                saved.getFailureCode(),
                saved.getMaskedKey(),
                saved.getMaskedDocument(),
                saved.getKeyHash()
            )
        );

        return saved;
    }

    private void requireWithinLimit(
        BigDecimal amount
    ) {
        if (amount.compareTo(
            properties.maxPerOperation()
        ) > 0) {

            throw new BusinessException(
                "PIX_LIMIT_EXCEEDED",
                "pix.limit-exceeded"
            );
        }
    }

    private static List<UUID> accountIds(
        PixPayment payment
    ) {
        if (payment.getDestinationScope()
            == PixDestinationScope.INTERNAL) {

            return List.of(
                payment.getOriginAccountId(),
                requireDestinationAccountId(
                    payment
                )
            );
        }

        return List.of(
            payment.getOriginAccountId()
        );
    }

    private static UUID requireDestinationAccountId(
        PixPayment payment
    ) {
        UUID destinationId =
            payment.getDestinationAccountId();

        if (destinationId == null) {
            throw new BusinessException(
                "PIX_TARGET_ACCOUNT_REQUIRED",
                "account.unavailable"
            );
        }

        return destinationId;
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