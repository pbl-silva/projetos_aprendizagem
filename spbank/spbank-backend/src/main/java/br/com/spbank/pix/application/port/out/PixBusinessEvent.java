package br.com.spbank.pix.application.port.out;

import br.com.spbank.pix.application.model.PixStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PixBusinessEvent(
    UUID paymentId,
    PixStatus status,
    UUID sourceAccountId,
    BigDecimal amount,
    Instant occurredAt,
    String failureCode,
    String maskedKey,
    String maskedDocument,
    String keyHash
) {
}