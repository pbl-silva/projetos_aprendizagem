package br.com.spbank.administracao.application.model;

import br.com.spbank.shared.application.model.CustomerPlan;

import java.time.Instant;
import java.util.UUID;

public record CustomerPlanChange(
        UUID id,
        UUID customerId,
        UUID administratorId,
        String administratorName,
        CustomerPlan previousPlan,
        CustomerPlan newPlan,
        String reason,
        Instant changedAt
) {
}