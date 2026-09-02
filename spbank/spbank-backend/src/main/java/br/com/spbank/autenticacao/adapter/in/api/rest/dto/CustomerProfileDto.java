package br.com.spbank.autenticacao.adapter.in.api.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerProfileDto(
        UUID id,
        String fullName,
        String cpf,
        LocalDate birthDate,
        String mobile,
        String email,
        CustomerAddressDto address
) {

    @Override
    public String toString() {
        return "CustomerProfileDto[id=" + id
                + ", fullName=" + fullName
                + ", cpf=[REDACTED]"
                + ", birthDate=[REDACTED]"
                + ", mobile=[REDACTED]"
                + ", email=[REDACTED]"
                + ", address=[REDACTED]]";
    }
}