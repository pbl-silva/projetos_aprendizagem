package br.com.spbank.administracao.application.model;

import br.com.spbank.conta.application.model.AccountPlan;

import java.time.Instant;
import java.util.UUID;

public record AccountPlanChange(
        UUID id,
        UUID accountId,
        UUID administratorId,
        String administratorName,
        AccountPlan previousPlan,
        AccountPlan newPlan,
        String reason,
        Instant changedAt
) {
}