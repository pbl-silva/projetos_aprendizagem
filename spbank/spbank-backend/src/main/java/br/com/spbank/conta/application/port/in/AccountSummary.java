package br.com.spbank.conta.application.port.in;

import br.com.spbank.conta.application.model.AccountPlan;
import br.com.spbank.conta.application.model.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummary(
        UUID id,
        String holderName,
        String bankCode,
        String branch,
        String accountNumber,
        AccountType accountType,
        AccountPlan accountPlan,
        BigDecimal balance
) {
}