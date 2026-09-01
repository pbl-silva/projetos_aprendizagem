package br.com.spbank.autenticacao.application.model;

import java.util.UUID;

public record Customer(
        UUID id,
        String fullName,
        String cpf,
        boolean active
) {

    @Override
    public String toString() {
        return "Customer[id=" + id
                + ", fullName=" + fullName
                + ", cpf=[REDACTED]"
                + ", active=" + active + "]";
    }
}