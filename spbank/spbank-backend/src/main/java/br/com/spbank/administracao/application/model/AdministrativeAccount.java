package br.com.spbank.administracao.application.model;

import br.com.spbank.conta.application.model.AccountPlan;
import br.com.spbank.conta.application.model.AccountType;

import java.util.UUID;

public record AdministrativeAccount(
        UUID accountId,
        String holderName,
        String maskedDocument,
        String branch,
        String accountNumber,
        AccountType accountType,
        AccountPlan accountPlan,
        boolean active
) {
}