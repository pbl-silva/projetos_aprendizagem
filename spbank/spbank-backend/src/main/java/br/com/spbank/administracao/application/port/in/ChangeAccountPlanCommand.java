package br.com.spbank.administracao.application.port.in;

import br.com.spbank.conta.application.model.AccountPlan;

public record ChangeAccountPlanCommand(
        AccountPlan accountPlan,
        String reason
) {
}