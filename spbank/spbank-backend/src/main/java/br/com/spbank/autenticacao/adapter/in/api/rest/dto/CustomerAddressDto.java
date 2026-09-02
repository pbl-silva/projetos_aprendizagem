package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import br.com.spbank.autenticacao.application.model.CustomerAddress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerAddressDto(

        @NotBlank(
                message = "{customer.address.postal-code.required}"
        )
        @Pattern(
                regexp = "^\\d{5}-?\\d{3}$",
                message = "{customer.address.postal-code.invalid}"
        )
        String postalCode,

        @NotBlank(
                message = "{customer.address.street.required}"
        )
        @Size(
                max = 120,
                message = "{customer.address.street.size}"
        )
        String street,

        @NotBlank(
                message = "{customer.address.number.required}"
        )
        @Size(
                max = 20,
                message = "{customer.address.number.size}"
        )
        String number,

        @Size(
                max = 80,
                message = "{customer.address.complement.size}"
        )
        String complement,

        @NotBlank(
                message = "{customer.address.district.required}"
        )
        @Size(
                max = 80,
                message = "{customer.address.district.size}"
        )
        String district,

        @NotBlank(
                message = "{customer.address.city.required}"
        )
        @Size(
                max = 80,
                message = "{customer.address.city.size}"
        )
        String city,

        @NotBlank(
                message = "{customer.address.state.required}"
        )
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "{customer.address.state.invalid}"
        )
        String state

) {

    public CustomerAddress toModel() {
        return new CustomerAddress(
                postalCode,
                street,
                number,
                complement,
                district,
                city,
                state
        );
    }

    public static CustomerAddressDto from(
            CustomerAddress address
    ) {
        return new CustomerAddressDto(
                address.postalCode(),
                address.street(),
                address.number(),
                address.complement(),
                address.district(),
                address.city(),
                address.state()
        );
    }

    @Override
    public String toString() {
        return "CustomerAddressDto[REDACTED]";
    }
}