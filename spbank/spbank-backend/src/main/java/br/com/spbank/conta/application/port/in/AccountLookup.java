package br.com.spbank.conta.application.port.in;

import br.com.spbank.conta.application.model.AccountType;

public record AccountLookup(
        String bankCode,
        String branch,
        String accountNumber,
        AccountType accountType
) {
}