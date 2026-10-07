package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixKeyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PixFavoriteRegistrationDto(
    @NotNull(message = "{pix.key.type.required}")
    PixKeyType keyType,

    @NotBlank(message = "{pix.key.required}")
    @Size(max = 120)
    String key,

    @Size(max = 120)
    String recipientName,

    @NotBlank(message = "{pix.favorite.bank.required}")
    @Size(max = 20)
    String bankCode,

    @Size(max = 24)
    String recipientDocument,

    @NotBlank(message = "{auth.confirmation.required}")
    String confirmationPassword
) {

    @Override
    public String toString() {
        return "PixFavoriteRegistrationDto[keyType="
            + keyType
            + ", key=[REDACTED], recipientName="
            + recipientName
            + ", bankCode="
            + bankCode
            + ", recipientDocument=[REDACTED]"
            + ", confirmationPassword=[REDACTED]]";
    }
}