package br.com.spbank.pix.application.model;

import java.time.Instant;
import java.util.UUID;

public record PixFavorite(
    UUID id,
    UUID sourceAccountId,
    UUID pixKeyId,
    String recipientName,
    String keyValue,
    String maskedKey,
    PixKeyType keyType,
    PixRouting routing,
    String bankCode,
    String bankName,
    String maskedRecipientDocument,
    Instant createdAt
) {

    public static PixFavorite internal(
        UUID sourceAccountId,
        UUID pixKeyId,
        String recipientName,
        String normalizedKey,
        String maskedKey,
        PixKeyType keyType,
        String maskedDocument,
        Instant now
    ) {
        return new PixFavorite(
            UUID.randomUUID(),
            sourceAccountId,
            pixKeyId,
            recipientName,
            normalizedKey,
            maskedKey,
            keyType,
            PixRouting.INTERNAL,
            "90001",
            "SPBank",
            maskedDocument,
            now
        );
    }

    public static PixFavorite external(
        UUID sourceAccountId,
        String recipientName,
        String normalizedKey,
        String maskedKey,
        PixKeyType keyType,
        String bankCode,
        String bankName,
        String maskedDocument,
        Instant now
    ) {
        return new PixFavorite(
            UUID.randomUUID(),
            sourceAccountId,
            null,
            recipientName,
            normalizedKey,
            maskedKey,
            keyType,
            PixRouting.EXTERNAL,
            bankCode,
            bankName,
            maskedDocument,
            now
        );
    }

    @Override
    public String toString() {
        return "PixFavorite[id=" + id
            + ", sourceAccountId=" + sourceAccountId
            + ", recipientName=" + recipientName
            + ", keyValue=[REDACTED]"
            + ", keyType=" + keyType
            + ", routing=" + routing
            + "]";
    }
}