package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.shared.validation.Cpf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegistrationRequestDto(

        @NotBlank(
                message = "{customer.name.required}"
        )
        @Size(
                max = 120,
                message = "{customer.name.size}"
        )
        String fullName,

        @NotBlank(
                message = "{customer.cpf.required}"
        )
        @Cpf
        String cpf,

        @NotNull(
                message = "{customer.birth-date.required}"
        )
        @Past(
                message = "{customer.birth-date.invalid}"
        )
        LocalDate birthDate,

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
        CustomerAddressDto address,

        @NotBlank(
                message = "{auth.username.required}"
        )
        @Size(
                min = 3,
                max = 60,
                message = "{auth.username.size}"
        )
        String username,

        @NotBlank(
                message = "{auth.password.required}"
        )
        @Size(
                min = 8,
                max = 72,
                message = "{auth.password.size}"
        )
        String password,

        @NotNull(
                message = "{account.type.required}"
        )
        AccountType accountType

) {

    @Override
    public String toString() {
        return "RegistrationRequestDto[fullName=" + fullName
                + ", cpf=[REDACTED]"
                + ", birthDate=[REDACTED]"
                + ", mobile=[REDACTED]"
                + ", email=[REDACTED]"
                + ", address=[REDACTED]"
                + ", username=" + username
                + ", password=[REDACTED]"
                + ", accountType=" + accountType + "]";
    }
}