package br.com.spbank.autenticacao.application.model;

import java.time.Instant;
import java.util.UUID;

public record LoginResult(
        String accessToken,
        Instant expiresAt,
        UUID accountId,
        String holderName
) {
}