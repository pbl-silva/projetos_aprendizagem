package br.com.spbank.autenticacao.application.port.in;

import br.com.spbank.autenticacao.application.model.CustomerAddress;
import br.com.spbank.conta.application.model.AccountType;

import java.time.LocalDate;

public record CustomerRegistrationCommand(
        String fullName,
        String cpf,
        LocalDate birthDate,
        String mobile,
        String email,
        CustomerAddress address,
        String username,
        String password,
        AccountType accountType
) {

    @Override
    public String toString() {
        return "CustomerRegistrationCommand[fullName=" + fullName
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