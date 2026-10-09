package br.com.spbank.administracao.adapter.in.api.rest.dto;

import br.com.spbank.administracao.application.model.AdministrativeCustomer;
import br.com.spbank.shared.application.model.CustomerPlan;

import java.util.List;
import java.util.UUID;

public record AdministrativeCustomerDto(
        UUID customerId,
        String fullName,
        String maskedDocument,
        CustomerPlan customerPlan,
        boolean active,
        List<AdministrativeAccountDto> accounts
) {

    public static AdministrativeCustomerDto from(
            AdministrativeCustomer customer
    ) {

        return new AdministrativeCustomerDto(
                customer.customerId(),
                customer.fullName(),
                customer.maskedDocument(),
                customer.customerPlan(),
                customer.active(),
                customer.accounts()
                        .stream()
                        .map(account ->
                                new AdministrativeAccountDto(
                                        account.accountId(),
                                        account.holderName(),
                                        account.maskedDocument(),
                                        account.branch(),
                                        account.accountNumber(),
                                        account.accountType(),
                                        account.active()
                                )
                        )
                        .toList()
        );
    }
}