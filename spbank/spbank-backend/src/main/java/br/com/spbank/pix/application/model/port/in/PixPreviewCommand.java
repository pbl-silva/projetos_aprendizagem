package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixKeyType;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PixPreviewCommand(
    UUID idempotencyKey,
    PixKeyType keyType,
    String keyValue,
    BigDecimal amount,
    UUID favoriteId,
    String scheduledFor,
    String recurrenceFrequency,
    Integer totalOccurrences,
    boolean favorite
) {

    public PixPreviewCommand {
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(keyValue);
        Objects.requireNonNull(amount);

        if (keyValue.isBlank()) {
            throw new IllegalArgumentException(
                "Chave PIX é obrigatória"
            );
        }

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                "Valor do PIX deve ser maior que zero"
            );
        }
    }
}