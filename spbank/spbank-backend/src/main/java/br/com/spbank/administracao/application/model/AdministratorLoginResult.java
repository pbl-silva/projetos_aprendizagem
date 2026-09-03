package br.com.spbank.administracao.application.model;

import java.time.Instant;
import java.util.UUID;

public record AdministratorLoginResult(
        String accessToken,
        Instant expiresAt,
        UUID administratorId,
        String displayName
) {

    @Override
    public String toString() {
        return "AdministratorLoginResult[accessToken=[REDACTED]"
                + ", expiresAt=" + expiresAt
                + ", administratorId=" + administratorId
                + ", displayName=" + displayName + "]";
    }
}