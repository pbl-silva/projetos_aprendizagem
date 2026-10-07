package br.com.spbank.pix.adapter.in.api.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PixConfirmationDto(
    @NotNull(message = "{pix.data.required}")
    @Valid
    PixCreationDto pix,

    @NotBlank(message = "{auth.confirmation.required}")
    String confirmationPassword
) {

    @Override
    public String toString() {
        return "PixConfirmationDto[pix="
            + pix
            + ", confirmationPassword=[REDACTED]]";
    }
}