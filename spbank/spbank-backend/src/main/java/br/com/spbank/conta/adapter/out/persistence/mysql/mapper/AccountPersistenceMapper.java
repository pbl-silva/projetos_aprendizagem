package br.com.spbank.conta.adapter.out.persistence.mysql.mapper;

import br.com.spbank.conta.adapter.out.persistence.mysql.data.AccountData;
import br.com.spbank.conta.application.model.Account;

public final class AccountPersistenceMapper {

    private AccountPersistenceMapper() {
    }

    public static Account toDomain(
            AccountData data
    ) {
        return new Account(
                data.getId(),
                data.getCustomerId(),
                data.getHolderName(),
                data.getHolderDocument(),
                data.getBankCode(),
                data.getBranch(),
                data.getAccountNumber(),
                data.getAccountType(),
                data.getAccountPlan(),
                data.getBalance(),
                data.isActive()
        );
    }

    public static AccountData toData(
            Account account
    ) {
        return new AccountData(
                account.getId(),
                account.getCustomerId(),
                account.getHolderName(),
                account.getHolderDocument(),
                account.getBankCode(),
                account.getBranch(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getAccountPlan(),
                account.getBalance(),
                account.isActive()
        );
    }
}