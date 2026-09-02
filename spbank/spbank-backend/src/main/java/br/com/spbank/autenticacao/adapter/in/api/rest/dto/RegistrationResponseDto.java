package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import java.util.UUID;

public record RegistrationResponseDto(
        UUID customerId,
        UUID accountId
) {
}