package br.com.spbank.conta.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountPlan;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummaryDto(
        UUID id,
        String holderName,
        AccountPlan accountPlan,
        BigDecimal balance
) {
}