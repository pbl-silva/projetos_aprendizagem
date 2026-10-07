package br.com.spbank.pix.application.usecase;

import br.com.spbank.conta.application.exception.AccountInactiveException;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountEntry;
import br.com.spbank.conta.application.model.EntryDirection;
import br.com.spbank.conta.application.model.EntryReferenceType;
import br.com.spbank.conta.application.model.EntryType;
import br.com.spbank.conta.application.port.out.AccountEntryPersistence;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.model.PixRouting;
import br.com.spbank.pix.application.port.out.PixBusinessEvent;
import br.com.spbank.pix.application.port.out.PixBusinessLogPort;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.port.out.PixSettlementInstruction;
import br.com.spbank.pix.application.port.out.PixSettlementPort;
import br.com.spbank.pix.application.service.PixProperties;
import br.com.spbank.shared.application.exception.BusinessException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PixExecutionService {

    private final AccountPersistence accounts;
    private final AccountEntryPersistence entries;
    private final PixPersistence pix;
    private final PixSettlementPort settlement;
    private final PixBusinessLogPort logs;
    private final PixProperties properties;
    private final Clock clock;

    public PixExecutionService(
        AccountPersistence accounts,
        AccountEntryPersistence entries,
        PixPersistence pix,
        PixSettlementPort settlement,
        PixBusinessLogPort logs,
        PixProperties properties,
        Clock clock
    ) {
        this.accounts = accounts;
        this.entries = entries;
        this.pix = pix;
        this.settlement = settlement;
        this.logs = logs;
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
            payment.getRouting() == PixRouting.INTERNAL

                ? List.of(
                    payment.getSourceAccountId(),
                    payment.getTargetAccountId()
                )

                : List.of(
                    payment.getSourceAccountId()
                );

        Map<UUID, Account> locked =
            accounts.findAllForUpdate(
                accountIds
            );

        Account source =
            active(
                locked.get(
                    payment.getSourceAccountId()
                )
            );

        if (payment.getAmount().compareTo(
            properties.maxPerOperation()
        ) > 0) {

            throw new BusinessException(
                "PIX_LIMIT_EXCEEDED",
                "pix.limit-exceeded"
            );
        }

        source.requireAvailable(
            payment.getAmount()
        );

        Instant now =
            clock.instant();

        source.debit(
            payment.getAmount()
        );

        List<AccountEntry> batch;

        List<Account> changedAccounts;

        String settlementReference = null;

        if (payment.getRouting()
            == PixRouting.INTERNAL) {

            Account target =
                active(
                    locked.get(
                        payment.getTargetAccountId()
                    )
                );

            if (source.getId().equals(
                target.getId()
            )) {

                throw new BusinessException(
                    "PIX_SAME_ACCOUNT",
                    "pix.same-account"
                );
            }

            target.credit(
                payment.getAmount()
            );

            batch = List.of(
                debitEntry(
                    source,
                    payment,
                    now
                ),

                AccountEntry.create(
                    target,
                    payment.getId(),
                    EntryReferenceType.PIX,
                    EntryType.PIX_IN,
                    EntryDirection.CREDIT,
                    payment.getAmount(),
                    "Pix recebido de "
                        + source.getHolderName(),
                    now,
                    source.getHolderName(),
                    source.getBankCode(),
                    "PIX"
                )
            );

            changedAccounts =
                List.of(
                    source,
                    target
                );

        } else {

            batch =
                List.of(
                    debitEntry(
                        source,
                        payment,
                        now
                    )
                );

            changedAccounts =
                List.of(
                    source
                );

            settlementReference =
                settlement.settle(
                    new PixSettlementInstruction(
                        payment.getId(),
                        source.getId(),
                        payment.getKeyType(),
                        payment.getKeyHash(),
                        payment.getDestinationBankCode(),
                        payment.getAmount()
                    )
                );
        }

        payment.complete(
            now,
            settlementReference
        );

        accounts.saveAll(
            changedAccounts
        );

        entries.saveAll(
            batch
        );

        PixPayment saved =
            pix.savePayment(
                payment
            );

        publish(
            saved
        );

        return saved;
    }

    private static AccountEntry debitEntry(
        Account source,
        PixPayment payment,
        Instant now
    ) {
        return AccountEntry.create(
            source,
            payment.getId(),
            EntryReferenceType.PIX,
            EntryType.PIX_OUT,
            EntryDirection.DEBIT,
            payment.getAmount(),
            "Pix enviado para "
                + payment.getRecipientName(),
            now,
            payment.getRecipientName(),
            payment.getDestinationBankCode(),
            "PIX"
        );
    }

    private void publish(
        PixPayment payment
    ) {
        logs.publish(
            new PixBusinessEvent(
                payment.getId(),
                payment.getStatus(),
                payment.getSourceAccountId(),
                payment.getAmount(),
                payment.getKeyType(),
                payment.getKeyHash(),
                clock.instant(),
                payment.getFailureCode()
            )
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