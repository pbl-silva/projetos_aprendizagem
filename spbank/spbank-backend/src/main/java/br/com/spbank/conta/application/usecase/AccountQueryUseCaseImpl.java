package br.com.spbank.conta.application.usecase;

import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountEntry;
import br.com.spbank.conta.application.port.in.AccountSummary;
import br.com.spbank.conta.application.port.in.GetAccountSummaryUseCase;
import br.com.spbank.conta.application.port.in.ListAccountEntriesUseCase;
import br.com.spbank.conta.application.port.out.AccountEntryPersistence;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.shared.application.port.out.CustomerPlanLookup;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountQueryUseCaseImpl
        implements GetAccountSummaryUseCase,
                   ListAccountEntriesUseCase {

    private final AccountPersistence accounts;
    private final AccountEntryPersistence entries;
    private final CustomerPlanLookup plans;

    public AccountQueryUseCaseImpl(
            AccountPersistence accounts,
            AccountEntryPersistence entries,
            CustomerPlanLookup plans
    ) {
        this.accounts = accounts;
        this.entries = entries;
        this.plans = plans;
    }

    @Override
    public AccountSummary get(UUID id) {

        Account account = accounts
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "ACCOUNT_NOT_FOUND"
                        )
                );

        return new AccountSummary(
                account.getId(),
                account.getHolderName(),
                account.getBankCode(),
                account.getBranch(),
                account.getAccountNumber(),
                account.getAccountType(),
                plans.findPlan(account.getCustomerId()),
                account.getBalance()
        );
    }

    @Override
    public List<AccountEntry> list(
            UUID id,
            int limit
    ) {

        accounts.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "ACCOUNT_NOT_FOUND"
                        )
                );

        if (limit < 1 || limit > 100) {
            limit = 50;
        }

        return entries.findRecent(
                id,
                limit
        );
    }
}