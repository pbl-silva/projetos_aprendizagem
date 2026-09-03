package br.com.spbank.administracao.adapter.in.api.rest.dto;

import br.com.spbank.administracao.application.model.AccountPlanChange;
import br.com.spbank.conta.application.model.AccountPlan;

import java.time.Instant;
import java.util.UUID;

public record AccountPlanChangeDto(
        UUID id,
        UUID accountId,
        UUID administratorId,
        String administratorName,
        AccountPlan previousPlan,
        AccountPlan newPlan,
        String reason,
        Instant changedAt
) {

    public static AccountPlanChangeDto from(
            AccountPlanChange change
    ) {

        return new AccountPlanChangeDto(
                change.id(),
                change.accountId(),
                change.administratorId(),
                change.administratorName(),
                change.previousPlan(),
                change.newPlan(),
                change.reason(),
                change.changedAt()
        );
    }
}