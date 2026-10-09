package br.com.spbank.administracao.adapter.in.api.rest.dto;

import br.com.spbank.administracao.application.model.CustomerPlanChange;
import br.com.spbank.shared.application.model.CustomerPlan;

import java.time.Instant;
import java.util.UUID;

public record CustomerPlanChangeDto(
        UUID id,
        UUID customerId,
        UUID administratorId,
        String administratorName,
        CustomerPlan previousPlan,
        CustomerPlan newPlan,
        String reason,
        Instant changedAt
) {

    public static CustomerPlanChangeDto from(
            CustomerPlanChange change
    ) {

        return new CustomerPlanChangeDto(
                change.id(),
                change.customerId(),
                change.administratorId(),
                change.administratorName(),
                change.previousPlan(),
                change.newPlan(),
                change.reason(),
                change.changedAt()
        );
    }
}