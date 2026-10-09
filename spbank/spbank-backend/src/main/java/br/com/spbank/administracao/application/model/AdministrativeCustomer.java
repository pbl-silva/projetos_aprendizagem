package br.com.spbank.administracao.application.model;

import br.com.spbank.shared.application.model.CustomerPlan;

import java.util.List;
import java.util.UUID;

public record AdministrativeCustomer(
        UUID customerId,
        String fullName,
        String maskedDocument,
        CustomerPlan customerPlan,
        boolean active,
        List<AdministrativeAccount> accounts
) {
    public AdministrativeCustomer {
        accounts = List.copyOf(accounts);
    }
}