package br.com.spbank.administracao.application.port.in;

import br.com.spbank.shared.application.model.CustomerPlan;

public record ChangeCustomerPlanCommand(
        CustomerPlan customerPlan,
        String reason
) {
}