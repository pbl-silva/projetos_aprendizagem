package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import java.util.UUID;

public record CurrentUserDto(
        UUID accountId,
        String holderName
) {
}