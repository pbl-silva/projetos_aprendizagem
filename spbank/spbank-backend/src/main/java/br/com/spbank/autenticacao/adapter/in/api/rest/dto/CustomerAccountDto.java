package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerAccountDto(
        UUID id,
        AccountType accountType,
        String branch,
        String accountNumber,
        BigDecimal balance,
        boolean selected
) {

    @Override
    public String toString() {
        return "CustomerAccountDto[id=" + id
                + ", accountType=" + accountType
                + ", branch=[REDACTED]"
                + ", accountNumber=[REDACTED]"
                + ", balance=" + balance
                + ", selected=" + selected + "]";
    }
}