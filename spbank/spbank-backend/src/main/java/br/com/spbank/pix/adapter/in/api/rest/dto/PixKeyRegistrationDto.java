package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixKeyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PixKeyRegistrationDto(
    @NotNull(message = "{pix.key.type.required}")
    PixKeyType type,

    @Size(max = 120)
    String value,

    @NotBlank(message = "{auth.confirmation.required}")
    String confirmationPassword
) {

    @Override
    public String toString() {
        return "PixKeyRegistrationDto[type="
            + type
            + ", value=[REDACTED]"
            + ", confirmationPassword=[REDACTED]]";
    }
}