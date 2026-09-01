package br.com.spbank.autenticacao.application.model;

import java.util.UUID;

public record AuthenticatedSession(
        UUID customerId,
        UUID accountId
) {
}