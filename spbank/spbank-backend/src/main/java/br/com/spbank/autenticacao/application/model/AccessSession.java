package br.com.spbank.autenticacao.application.model;

import java.time.Instant;
import java.util.UUID;

public record AccessSession(
        UUID id,
        UUID customerId,
        UUID accountId,
        String tokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {

    public boolean isValidAt(
            Instant instant
    ) {
        return revokedAt == null
                && expiresAt.isAfter(instant);
    }

    @Override
    public String toString() {
        return "AccessSession[id=" + id
                + ", customerId=" + customerId
                + ", accountId=" + accountId
                + ", tokenHash=[REDACTED]"
                + ", createdAt=" + createdAt
                + ", expiresAt=" + expiresAt
                + ", revokedAt=" + revokedAt + "]";
    }
}