package br.com.spbank.shared.application.port.out;

import br.com.spbank.shared.application.model.CustomerPlan;

import java.util.UUID;

public interface CustomerPlanLookup {

    CustomerPlan findPlan(UUID customerId);

    CustomerPlan findPlanForUpdate(UUID customerId);
}