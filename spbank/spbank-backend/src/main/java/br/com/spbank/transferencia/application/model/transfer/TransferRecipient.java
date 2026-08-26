package br.com.spbank.transferencia.application.model.transfer;

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