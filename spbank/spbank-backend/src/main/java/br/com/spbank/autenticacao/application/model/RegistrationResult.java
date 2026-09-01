package br.com.spbank.autenticacao.application.model;

import java.util.UUID;

public record RegistrationResult(
        UUID customerId,
        UUID accountId
) {
}