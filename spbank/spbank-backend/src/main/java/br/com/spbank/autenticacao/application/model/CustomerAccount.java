package br.com.spbank.autenticacao.application.model;

import br.com.spbank.conta.application.model.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerAccount(
        UUID id,
        AccountType accountType,
        String branch,
        String accountNumber,
        BigDecimal balance,
        boolean selected
) {

    @Override
    public String toString() {
        return "CustomerAccount[id=" + id
                + ", accountType=" + accountType
                + ", branch=[REDACTED]"
                + ", accountNumber=[REDACTED]"
                + ", balance=" + balance
                + ", selected=" + selected + "]";
    }
}