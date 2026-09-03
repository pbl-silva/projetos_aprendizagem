package br.com.spbank.administracao.adapter.in.api.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record AdministratorLoginResponseDto(
        String accessToken,
        Instant expiresAt,
        UUID administratorId,
        String displayName
) {

    @Override
    public String toString() {
        return "AdministratorLoginResponseDto[accessToken=[REDACTED]"
                + ", expiresAt=" + expiresAt
                + ", administratorId=" + administratorId
                + ", displayName=" + displayName + "]";
    }
}