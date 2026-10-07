package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixKeyType;

import java.time.Instant;
import java.util.UUID;

public record PixKeyDto(
    UUID id,
    PixKeyType type,
    String value,
    Instant createdAt
) {

    @Override
    public String toString() {
        return "PixKeyDto[id="
            + id
            + ", type="
            + type
            + ", value=[REDACTED], createdAt="
            + createdAt
            + "]";
    }
}