package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountType;

import jakarta.validation.constraints.NotNull;

public record OpenAccountRequestDto(

        @NotNull(
                message = "{account.type.required}"
        )
        AccountType accountType

) {
}