package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

        @NotBlank(
                message = "{auth.username.required}"
        )
        String username,

        @NotBlank(
                message = "{auth.password.required}"
        )
        String password

) {

    @Override
    public String toString() {
        return "LoginRequestDto[username=" + username
                + ", password=[REDACTED]]";
    }
}