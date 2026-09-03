package br.com.spbank.administracao.application.port.out;

import br.com.spbank.administracao.application.model.AccountPlanChange;
import br.com.spbank.administracao.application.model.AdministratorCredential;
import br.com.spbank.administracao.application.model.AdministratorSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdministrationPersistence {

    Optional<AdministratorCredential> findCredential(
            String normalizedUsername
    );

    Optional<AdministratorCredential> findCredentialById(
            UUID administratorId
    );

    Optional<AdministratorSession> findSession(
            String tokenHash
    );

    void saveSession(
            AdministratorSession session
    );

    void revokeSession(
            String tokenHash,
            Instant revokedAt
    );

    void savePlanChange(
            AccountPlanChange change
    );

    List<AccountPlanChange> findPlanChanges(
            UUID accountId
    );
}