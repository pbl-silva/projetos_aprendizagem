package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixKeyType;

import java.util.Objects;
import java.util.UUID;

public record PixFavoriteRegistrationCommand(
    UUID sourceAccountId,
    PixKeyType keyType,
    String keyValue,
    String recipientName,
    String bankCode,
    String recipientDocument
) {

    public PixFavoriteRegistrationCommand {
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
            bankCode,
            "bankCode"
        );

        if (keyValue.isBlank()) {
            throw new IllegalArgumentException(
                "Chave PIX é obrigatória"
            );
        }

        if (bankCode.isBlank()) {
            throw new IllegalArgumentException(
                "Instituição é obrigatória"
            );
        }
    }

    @Override
    public String toString() {
        return "PixFavoriteRegistrationCommand[sourceAccountId="
            + sourceAccountId
            + ", keyType="
            + keyType
            + ", keyValue=[REDACTED]"
            + ", recipientName="
            + recipientName
            + ", bankCode="
            + bankCode
            + ", recipientDocument=[REDACTED]]";
    }
}