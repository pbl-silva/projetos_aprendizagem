package br.com.spbank.pix.application.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PixFavorite {

    private final UUID id;
    private final UUID originAccountId;
    private final UUID pixKeyId;
    private final String normalizedKeyValue;
    private final PixDestinationScope destinationScope;
    private final String holderName;
    private final String maskedKey;
    private final PixKeyType keyType;
    private final String bankCode;
    private final String bankName;
    private final String maskedDocument;
    private final Instant createdAt;

    public PixFavorite(
        UUID id,
        UUID originAccountId,
        UUID pixKeyId,
        String normalizedKeyValue,
        PixDestinationScope destinationScope,
        String holderName,
        String maskedKey,
        PixKeyType keyType,
        String bankCode,
        String bankName,
        String maskedDocument,
        Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.originAccountId = Objects.requireNonNull(originAccountId);
        this.normalizedKeyValue = Objects.requireNonNull(normalizedKeyValue);
        this.destinationScope = Objects.requireNonNull(destinationScope);
        this.holderName = Objects.requireNonNull(holderName);
        this.maskedKey = Objects.requireNonNull(maskedKey);
        this.keyType = Objects.requireNonNull(keyType);
        this.bankCode = Objects.requireNonNull(bankCode);
        this.bankName = Objects.requireNonNull(bankName);
        this.createdAt = Objects.requireNonNull(createdAt);

        if (destinationScope == PixDestinationScope.INTERNAL
            && pixKeyId == null) {
            throw new IllegalArgumentException(
                "Chave PIX é obrigatória para favorito interno"
            );
        }

        if (destinationScope == PixDestinationScope.EXTERNAL
            && pixKeyId != null) {
            throw new IllegalArgumentException(
                "Chave PIX não deve ser informada para favorito externo"
            );
        }

        if (destinationScope == PixDestinationScope.INTERNAL
            && (maskedDocument == null || maskedDocument.isBlank())) {
            throw new IllegalArgumentException(
                "Documento mascarado é obrigatório para favorito interno"
            );
        }

        this.pixKeyId = pixKeyId;
        this.maskedDocument = maskedDocument;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOriginAccountId() {
        return originAccountId;
    }

    public UUID getPixKeyId() {
        return pixKeyId;
    }

    public String getNormalizedKeyValue() {
        return normalizedKeyValue;
    }

    public PixDestinationScope getDestinationScope() {
        return destinationScope;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getMaskedKey() {
        return maskedKey;
    }

    public PixKeyType getKeyType() {
        return keyType;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public String getMaskedDocument() {
        return maskedDocument;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isInternal() {
        return destinationScope == PixDestinationScope.INTERNAL;
    }

    public boolean isExternal() {
        return destinationScope == PixDestinationScope.EXTERNAL;
    }

    public static PixFavorite external(
        UUID originAccountId,
        String holderName,
        String normalizedKeyValue,
        String maskedKey,
        PixKeyType keyType,
        String bankCode,
        String bankName,
        String maskedDocument,
        Instant createdAt
    ) {
        return new PixFavorite(
            UUID.randomUUID(),
            originAccountId,
            null,
            normalizedKeyValue,
            PixDestinationScope.EXTERNAL,
            holderName,
            maskedKey,
            keyType,
            bankCode,
            bankName,
            maskedDocument,
            createdAt
        );
    }

    @Override
    public String toString() {
        return "PixFavorite[" +
            "id=" + id +
            ", originAccountId=" + originAccountId +
            ", pixKeyId=" + pixKeyId +
            ", normalizedKeyValue=[REDACTED]" +
            ", destinationScope=" + destinationScope +
            ", holderName=" + holderName +
            ", maskedKey=" + maskedKey +
            ", keyType=" + keyType +
            ", bankCode=" + bankCode +
            ", bankName=" + bankName +
            ", maskedDocument=" + maskedDocument +
            ", createdAt=" + createdAt +
            ']';
    }
}