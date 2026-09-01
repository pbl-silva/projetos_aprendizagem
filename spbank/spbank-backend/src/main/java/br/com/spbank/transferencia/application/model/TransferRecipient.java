package br.com.spbank.transferencia.application.model;

import br.com.spbank.conta.application.model.AccountType;

public record TransferRecipient(
        String name,
        String document,
        String bankCode,
        String branch,
        String accountNumber,
        AccountType accountType
) {
}