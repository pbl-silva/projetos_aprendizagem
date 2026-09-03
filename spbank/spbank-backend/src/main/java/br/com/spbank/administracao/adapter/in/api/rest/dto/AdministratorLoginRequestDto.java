package br.com.spbank.administracao.adapter.in.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AdministratorLoginRequestDto(
        @NotBlank(message = "{auth.username.required}")
        String username,

        @NotBlank(message = "{auth.password.required}")
        String password
) {

    @Override
    public String toString() {
        return "AdministratorLoginRequestDto[username="
                + username
                + ", password=[REDACTED]]";
    }
}