package br.com.spbank.autenticacao.application.model;

import java.time.LocalDate;
import java.util.UUID;

public record Customer(
        UUID id,
        String fullName,
        String cpf,
        LocalDate birthDate,
        String mobile,
        String email,
        CustomerAddress address,
        boolean active
) {

    public Customer withContact(
            String newMobile,
            String newEmail,
            CustomerAddress newAddress
    ) {
        return new Customer(
                id,
                fullName,
                cpf,
                birthDate,
                newMobile,
                newEmail,
                newAddress,
                active
        );
    }

    @Override
    public String toString() {
        return "Customer[id=" + id
                + ", fullName=" + fullName
                + ", cpf=[REDACTED]"
                + ", birthDate=[REDACTED]"
                + ", mobile=[REDACTED]"
                + ", email=[REDACTED]"
                + ", address=[REDACTED]"
                + ", active=" + active + "]";
    }
}