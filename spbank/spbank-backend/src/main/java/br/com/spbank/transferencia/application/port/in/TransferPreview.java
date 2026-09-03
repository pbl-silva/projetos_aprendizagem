package br.com.spbank.transferencia.application.port.in;

import br.com.spbank.transferencia.application.model.TransferType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferPreview(
        TransferType type,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal total,
        LocalDate effectiveDate,
        String institutionName,
        int remainingFreeTeds
) {
}