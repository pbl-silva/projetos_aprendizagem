package br.com.spbank.administracao.application.model;

import java.util.UUID;

public record AdministratorCredential(
        UUID id,
        String displayName,
        String username,
        String passwordHash,
        boolean active
) {

    @Override
    public String toString() {
        return "AdministratorCredential[id=" + id
                + ", displayName=" + displayName
                + ", username=" + username
                + ", passwordHash=[REDACTED]"
                + ", active=" + active + "]";
    }
}