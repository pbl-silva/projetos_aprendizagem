package br.com.spbank.administracao.application.model;

import java.time.Instant;
import java.util.UUID;

public record AdministratorSession(
        UUID id,
        UUID administratorId,
        String tokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {

    public boolean isValidAt(Instant instant) {
        return revokedAt == null
                && expiresAt.isAfter(instant);
    }

    @Override
    public String toString() {
        return "AdministratorSession[id=" + id
                + ", administratorId=" + administratorId
                + ", tokenHash=[REDACTED]"
                + ", createdAt=" + createdAt
                + ", expiresAt=" + expiresAt
                + ", revokedAt=" + revokedAt + "]";
    }
}