package br.com.spbank.conta.adapter.in.api.rest.mapper;

import br.com.spbank.conta.adapter.in.api.rest.dto.*;
import br.com.spbank.conta.application.model.AccountEntry;
import br.com.spbank.conta.application.port.in.AccountSummary;

public final class AccountRestMapper {

    private AccountRestMapper() {
    }

    public static AccountSummaryDto toDto(
            AccountSummary summary
    ) {

        return new AccountSummaryDto(
                summary.id(),
                summary.holderName(),
                summary.accountPlan(),
                summary.balance()
        );
    }

    public static AccountEntryDto toDto(
            AccountEntry entry
    ) {

        return new AccountEntryDto(
                entry.id(),
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