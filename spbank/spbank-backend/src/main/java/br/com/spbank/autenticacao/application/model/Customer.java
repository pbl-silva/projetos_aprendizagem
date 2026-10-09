package br.com.spbank.autenticacao.application.model;

import br.com.spbank.shared.application.model.CustomerPlan;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Customer(
        UUID id,
        String fullName,
        String cpf,
        LocalDate birthDate,
        String mobile,
        String email,
        CustomerAddress address,
        CustomerPlan plan,
        boolean active
) {

    public Customer {
        Objects.requireNonNull(plan, "plan");
    }

    public Customer withPlan(CustomerPlan newPlan) {
        return new Customer(
                id,
                fullName,
                cpf,
                birthDate,
                mobile,
                email,
                address,
                newPlan,
                active
        );
    }

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
                plan,
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