package br.com.spbank.transferencia.application.port.out;

import br.com.spbank.transferencia.application.model.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferBusinessEvent(
        UUID transferId,
        TransferStatus status,
        UUID sourceAccountId,
        BigDecimal amount,
        BigDecimal fee,
        Instant occurredAt,
        String failureCode
) {
}