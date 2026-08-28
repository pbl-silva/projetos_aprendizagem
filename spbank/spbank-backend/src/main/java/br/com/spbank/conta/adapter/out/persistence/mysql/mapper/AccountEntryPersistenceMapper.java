package br.com.spbank.conta.adapter.out.persistence.mysql.mapper;

import br.com.spbank.conta.adapter.out.persistence.mysql.data.AccountEntryData;
import br.com.spbank.conta.application.model.AccountEntry;

public final class AccountEntryPersistenceMapper {

    private AccountEntryPersistenceMapper() {
    }

    public static AccountEntry toDomain(
            AccountEntryData data
    ) {

        return new AccountEntry(
                data.getId(),
                data.getAccountId(),
                data.getReferenceId(),
                data.getReferenceType(),
                data.getType(),
                data.getDirection(),
                data.getAmount(),
                data.getBalanceAfter(),
                data.getDescription(),
                data.getOccurredAt(),
                data.getCounterpartyName(),
                data.getCounterpartyBankCode(),
                data.getOperationType()
        );
    }

    public static AccountEntryData toData(
            AccountEntry entry
    ) {

        return new AccountEntryData(
                entry.id(),
                entry.accountId(),
                entry.referenceId(),
                entry.referenceType(),
                entry.type(),
                entry.direction(),
                entry.amount(),
                entry.balanceAfter(),
                entry.description(),
                entry.occurredAt(),
                entry.counterpartyName(),
                entry.counterpartyBankCode(),
                entry.operationType()
        );
    }
}