package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixRouting;

import java.time.Instant;
import java.util.UUID;

public record PixFavoriteDto(
    UUID id,
    UUID pixKeyId,
    String recipientName,
    String key,
    String maskedKey,
    PixKeyType keyType,
    PixRouting routing,
    String bankCode,
    String bankName,
    String maskedRecipientDocument,
    Instant createdAt
) {

    @Override
    public String toString() {
        return "PixFavoriteDto[id="
            + id
            + ", recipientName="
            + recipientName
            + ", key=[REDACTED], keyType="
            + keyType
            + "]";
    }
}