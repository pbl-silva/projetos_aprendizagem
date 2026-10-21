
package br.com.spbank.cartao.adapter.in.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreditAcceptanceDto(

        @NotBlank
        @Size(max = 128)
        String confirmationPassword

) {

    @Override
    public String toString() {

        return "CreditAcceptanceDto[confirmationPassword=[REDACTED]]";
    }
}
