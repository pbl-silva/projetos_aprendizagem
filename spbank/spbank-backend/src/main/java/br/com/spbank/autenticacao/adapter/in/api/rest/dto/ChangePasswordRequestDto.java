package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(

        @NotBlank(
                message = "{auth.current-password.required}"
        )
        String currentPassword,

        @NotBlank(
                message = "{auth.new-password.required}"
        )
        @Size(
                min = 8,
                max = 72,
                message = "{auth.password.size}"
        )
        String newPassword,

        @NotBlank(
                message = "{auth.new-password-confirmation.required}"
        )
        String newPasswordConfirmation

) {

    @Override
    public String toString() {
        return "ChangePasswordRequestDto[currentPassword=[REDACTED]"
                + ", newPassword=[REDACTED]"
                + ", newPasswordConfirmation=[REDACTED]]";
    }
}