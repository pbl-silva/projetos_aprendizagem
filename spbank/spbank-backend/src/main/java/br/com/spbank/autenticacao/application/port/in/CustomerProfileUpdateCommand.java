package br.com.spbank.autenticacao.application.port.in;

import br.com.spbank.autenticacao.application.model.CustomerAddress;

public record CustomerProfileUpdateCommand(
        String mobile,
        String email,
        CustomerAddress address
) {

    @Override
    public String toString() {
        return "CustomerProfileUpdateCommand[mobile=[REDACTED]"
                + ", email=[REDACTED]"
                + ", address=[REDACTED]]";
    }
}