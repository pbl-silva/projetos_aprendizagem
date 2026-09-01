package br.com.spbank.autenticacao.application.model;

import java.util.UUID;

public record AccessCredential(
        UUID customerId,
        String username,
        String passwordHash,
        boolean active
) {

    @Override
    public String toString() {
        return "AccessCredential[customerId=" + customerId
                + ", username=" + username
                + ", passwordHash=[REDACTED]"
                + ", active=" + active + "]";
    }
}