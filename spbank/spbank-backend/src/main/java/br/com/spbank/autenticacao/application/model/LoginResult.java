package br.com.spbank.autenticacao.application.model;

import java.time.Instant;
import java.util.UUID;

public record LoginResult(
        String accessToken,
        Instant expiresAt,
        UUID accountId,
        String holderName
) {

    @Override
    public String toString() {
        return "LoginResult[accessToken=[REDACTED]"
                + ", expiresAt=" + expiresAt
                + ", accountId=" + accountId
                + ", holderName=" + holderName + "]";
    }
}