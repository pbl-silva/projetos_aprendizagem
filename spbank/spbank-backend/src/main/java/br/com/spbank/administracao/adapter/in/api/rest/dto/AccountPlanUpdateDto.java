    package br.com.spbank.administracao.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountPlan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountPlanUpdateDto(

        @NotNull(message = "{admin.account-plan.required}")
        AccountPlan accountPlan,

        @NotBlank(message = "{admin.account-plan.reason-required}")
        @Size(
                max = 240,
                message = "{admin.account-plan.reason-size}"
        )
        String reason
) {
}