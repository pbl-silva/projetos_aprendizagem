package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixRecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record PixPreviewCommand(
    UUID sourceAccountId,
    PixKeyType keyType,
    String keyValue,
    BigDecimal amount,
    LocalDate scheduledFor,
    PixRecurrenceFrequency recurrenceFrequency,
    Integer totalOccurrences,
    boolean saveFavorite,
    String recipientName,
    String bankCode,
    String recipientDocument
) {

    public PixPreviewCommand {
        Objects.requireNonNull(
            sourceAccountId,
            "sourceAccountId"
        );

        Objects.requireNonNull(
            keyType,
            "keyType"
        );

        Objects.requireNonNull(
            keyValue,
            "keyValue"
        );

        Objects.requireNonNull(
            amount,
            "amount"
        );

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

    @Override
    public String toString() {
        return "PixPreviewCommand[sourceAccountId="
            + sourceAccountId
            + ", keyType="
            + keyType
            + ", keyValue=[REDACTED]"
            + ", amount="
            + amount
            + ", scheduledFor="
            + scheduledFor
            + ", recurrenceFrequency="
            + recurrenceFrequency
            + ", totalOccurrences="
            + totalOccurrences
            + ", saveFavorite="
            + saveFavorite
            + ", recipientName="
            + recipientName
            + ", bankCode="
            + bankCode
            + ", recipientDocument=[REDACTED]"
            + "]";
    }
}