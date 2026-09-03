package br.com.spbank.transferencia.adapter.in.api.rest.dto;

import br.com.spbank.transferencia.application.model.TransferType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferPreviewDto(
        TransferType type,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal total,
        LocalDate effectiveDate,
        String institutionName,
        int remainingFreeTeds
) {
}