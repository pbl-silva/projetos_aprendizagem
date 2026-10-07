package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixFrequency;
import br.com.spbank.pix.application.model.PixKeyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PixCreationCommand(
    UUID sourceAccountId,
    UUID idempotencyKey,
    PixKeyType keyType,
    String key,
    BigDecimal amount,
    LocalDate scheduledFor,
    PixFrequency frequency,
    Integer occurrences,
    boolean saveFavorite
) {

    @Override
    public String toString() {
        return "PixCreationCommand[sourceAccountId=" + sourceAccountId
            + ", idempotencyKey=" + idempotencyKey
            + ", keyType=" + keyType
            + ", key=[REDACTED]"
            + ", amount=" + amount
            + ", scheduledFor=" + scheduledFor
            + ", frequency=" + frequency
            + ", occurrences=" + occurrences
            + ", saveFavorite=" + saveFavorite
            + "]";
    }
}