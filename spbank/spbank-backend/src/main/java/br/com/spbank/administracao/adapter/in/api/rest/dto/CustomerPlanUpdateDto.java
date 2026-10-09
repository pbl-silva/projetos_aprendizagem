package br.com.spbank.administracao.adapter.in.api.rest.dto;

import br.com.spbank.shared.application.model.CustomerPlan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerPlanUpdateDto(

        @NotNull(
                message = "{admin.customer-plan.required}"
        )
        CustomerPlan customerPlan,

        @NotBlank(
                message = "{admin.customer-plan.reason-required}"
        )
        @Size(
                max = 240,
                message = "{admin.customer-plan.reason-size}"
        )
        String reason

) {
}