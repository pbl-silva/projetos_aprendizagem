package br.com.spbank.transferencia.application.usecase.transfer;

import br.com.spbank.conta.application.exception.AccountInactiveException;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountEntry;
import br.com.spbank.conta.application.model.EntryDirection;
import br.com.spbank.conta.application.model.EntryReferenceType;
import br.com.spbank.conta.application.model.EntryType;
import br.com.spbank.conta.application.port.out.AccountEntryPersistence;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.transferencia.application.model.transfer.Transfer;
import br.com.spbank.transferencia.application.model.transfer.TransferType;
import br.com.spbank.transferencia.application.port.out.BusinessLogPort;
import br.com.spbank.transferencia.application.port.out.TedSettlementInstruction;
import br.com.spbank.transferencia.application.port.out.TedSettlementPort;
import br.com.spbank.transferencia.application.port.out.TransferBusinessEvent;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;
import br.com.spbank.transferencia.application.service.transfer.BusinessCalendar;
import br.com.spbank.transferencia.application.service.transfer.TedFeePolicy;
import br.com.spbank.transferencia.application.service.transfer.TransferPolicy;

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
public class TransferExecutionService {

    private final AccountPersistence accounts;
    private final TransferPersistence transfers;
    private final AccountEntryPersistence entries;
    private final BusinessLogPort businessLogs;
    private final TransferPolicy policy;
    private final TedFeePolicy fees;
    private final BusinessCalendar calendar;
    private final TedSettlementPort tedSettlement;
    private final Clock clock;

    public TransferExecutionService(
            AccountPersistence accounts,
            TransferPersistence transfers,
            AccountEntryPersistence entries,
            BusinessLogPort businessLogs,
            TransferPolicy policy,
            TedFeePolicy fees,
            BusinessCalendar calendar,
            TedSettlementPort tedSettlement,
            Clock clock
    ) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.entries = entries;
        this.businessLogs = businessLogs;
        this.policy = policy;
        this.fees = fees;
        this.calendar = calendar;
        this.tedSettlement = tedSettlement;
        this.clock = clock;
    }

    @Transactional(
            propagation = Propagation.MANDATORY,
            noRollbackFor = BusinessException.class
    )
    public Transfer executeLocked(Transfer transfer) {

        List<UUID> accountIds =
                accountIds(transfer);

        Map<UUID, Account> locked =
                accounts.findAllForUpdate(
                        accountIds
                );

        Account source = active(
                locked.get(
                        transfer.getSourceAccountId()
                )
        );

        Optional<Account> target =
                transfer.getType() == TransferType.INTERNAL

                        ? Optional.of(
                                active(
                                        locked.get(
                                                requireTargetAccountId(
                                                        transfer
                                                )
                                        )
                                )
                        )

                        : Optional.empty();

        BigDecimal amount =
                transfer.getAmount();

        Instant now =
                clock.instant();

        policy.requireWithinLimit(
                transfer.getType(),
                amount
        );

        long completedTeds =
                transfer.getType() == TransferType.TED

                        ? transfers.countCompletedBySourceAndTypeBetween(
                                source.getId(),
                                TransferType.TED,
                                calendar.monthStart(now),
                                calendar.nextMonthStart(now)
                        )

                        : 0;

        BigDecimal fee =
                transfer.getType() == TransferType.TED

                        ? fees.feeFor(
                                source,
                                completedTeds
                        )

                        : BigDecimal.ZERO.setScale(2);

        transfer.defineFee(fee);

        source.requireAvailable(
                amount.add(fee)
        );

        List<AccountEntry> batch =
                new ArrayList<>();

        source.debit(amount);

        batch.add(
                AccountEntry.create(
                        source,
                        transfer.getId(),
                        EntryReferenceType.TRANSFER,
                        EntryType.TRANSFER_OUT,
                        EntryDirection.DEBIT,
                        amount,

                        transfer.getType() == TransferType.TED
                                ? "TED enviada para "
                                    + transfer.getRecipient().name()
                                : "Transferência enviada para "
                                    + transfer.getRecipient().name(),

                        now,
                        transfer.getRecipient().name(),
                        transfer.getRecipient().bankCode(),
                        transfer.getType().name()
                )
        );

        if (fee.signum() > 0) {

            source.debit(fee);

            batch.add(
                    AccountEntry.create(
                            source,
                            transfer.getId(),
                            EntryReferenceType.TRANSFER,
                            EntryType.FEE,
                            EntryDirection.DEBIT,
                            fee,

                            transfer.getType() == TransferType.TED
                                    ? "Tarifa TED"
                                    : "Tarifa de transferência",

                            now,
                            transfer.getRecipient().name(),
                            transfer.getRecipient().bankCode(),
                            transfer.getType().name()
                    )
            );
        }

        if (transfer.getType() == TransferType.INTERNAL) {

            Account internalTarget =
                    target.orElseThrow();

            internalTarget.credit(amount);

            batch.add(
                    AccountEntry.create(
                            internalTarget,
                            transfer.getId(),
                            EntryReferenceType.TRANSFER,
                            EntryType.TRANSFER_IN,
                            EntryDirection.CREDIT,
                            amount,
                            "Transferência recebida de "
                                    + source.getHolderName(),
                            now,
                            source.getHolderName(),
                            source.getBankCode(),
                            transfer.getType().name()
                    )
            );

        } else {

            String reference =
                    tedSettlement.settle(
                            new TedSettlementInstruction(
                                    transfer.getId(),
                                    transfer.getSourceAccountId(),
                                    transfer.getRecipient(),
                                    amount
                            )
                    );

            transfer.registerSettlement(
                    reference
            );
        }

        transfer.complete(now);

        List<Account> changedAccounts =
                new ArrayList<>();

        changedAccounts.add(source);

        if (target.isPresent()) {
            changedAccounts.add(
                    target.orElseThrow()
            );
        }

        accounts.saveAll(
                changedAccounts
        );

        entries.saveAll(
                batch
        );

        Transfer saved =
                transfers.save(
                        transfer
                );

        businessLogs.publish(
                new TransferBusinessEvent(
                        saved.getId(),
                        saved.getStatus(),
                        saved.getSourceAccountId(),
                        saved.getAmount(),
                        saved.getFee(),
                        now,
                        saved.getFailureCode()
                )
        );

        return saved;
    }

    private static List<UUID> accountIds(
            Transfer transfer
    ) {

        if (transfer.getType() == TransferType.INTERNAL) {

            return List.of(
                    transfer.getSourceAccountId(),
                    requireTargetAccountId(transfer)
            );
        }

        return List.of(
                transfer.getSourceAccountId()
        );
    }

    private static UUID requireTargetAccountId(
            Transfer transfer
    ) {

        UUID targetId =
                transfer.getTargetAccountId();

        if (targetId == null) {

            throw new BusinessException(
                    "TARGET_ACCOUNT_REQUIRED",
                    "account.unavailable"
            );
        }

        return targetId;
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