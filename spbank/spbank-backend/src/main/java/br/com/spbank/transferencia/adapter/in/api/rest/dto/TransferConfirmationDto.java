package br.com.spbank.transferencia.adapter.in.api.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferConfirmationDto(

        @NotNull(message = "{transfer.data.required}")
        @Valid
        TransferCreationDto transfer,

        @NotBlank(message = "{auth.confirmation.required}")
        String confirmationPassword

) {

    @Override
    public String toString() {

        return "TransferConfirmationDto[transfer="
                + transfer
                + ", confirmationPassword=[REDACTED]]";
    }
}