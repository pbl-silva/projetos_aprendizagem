package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerProfileUpdateDto(

        @NotBlank(
                message = "{customer.mobile.required}"
        )
        @Pattern(
                regexp = "^(?:\\+?55[ ]?)?\\(?[1-9][0-9]\\)?[ ]?9[0-9]{4}[- ]?[0-9]{4}$",
                message = "{customer.mobile.invalid}"
        )
        String mobile,

        @NotBlank(
                message = "{customer.email.required}"
        )
        @Email(
                message = "{customer.email.invalid}"
        )
        @Size(
                max = 254,
                message = "{customer.email.size}"
        )
        String email,

        @NotNull(
                message = "{customer.address.required}"
        )
        @Valid
        CustomerAddressDto address

) {

    @Override
    public String toString() {
        return "CustomerProfileUpdateDto[mobile=[REDACTED]"
                + ", email=[REDACTED]"
                + ", address=[REDACTED]]";
    }
}