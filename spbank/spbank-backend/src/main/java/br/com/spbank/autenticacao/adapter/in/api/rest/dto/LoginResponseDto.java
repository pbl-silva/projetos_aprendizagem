package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginResponseDto(
        String accessToken,
        Instant expiresAt,
        UUID accountId,
        String holderName
) {

    @Override
    public String toString() {
        return "LoginResponseDto[accessToken=[REDACTED]"
                + ", expiresAt=" + expiresAt
                + ", accountId=" + accountId
                + ", holderName=" + holderName + "]";
    }
}