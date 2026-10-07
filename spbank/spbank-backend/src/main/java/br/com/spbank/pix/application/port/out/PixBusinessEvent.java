package br.com.spbank.pix.application.port.out;

import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PixBusinessEvent(
    UUID pixId,
    PixStatus status,
    UUID sourceAccountId,
    BigDecimal amount,
    PixKeyType keyType,
    String keyFingerprint,
    Instant occurredAt,
    String failureCode
) {
}