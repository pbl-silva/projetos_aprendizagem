package br.com.spbank.administracao.application.model;

import java.util.UUID;

public record AuthenticatedAdministrator(
        UUID id,
        String displayName
) {
}