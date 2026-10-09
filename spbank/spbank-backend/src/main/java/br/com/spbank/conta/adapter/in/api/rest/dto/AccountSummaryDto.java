package br.com.spbank.conta.adapter.in.api.rest.dto;

import br.com.spbank.shared.application.model.CustomerPlan;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummaryDto(
        UUID id,
        String holderName,
        CustomerPlan customerPlan,
        BigDecimal balance
) {
}