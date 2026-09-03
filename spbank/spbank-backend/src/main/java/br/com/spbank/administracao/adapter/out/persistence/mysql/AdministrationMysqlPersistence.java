package br.com.spbank.administracao.adapter.out.persistence.mysql;

import br.com.spbank.administracao.adapter.out.persistence.mysql.data.AccountPlanChangeData;
import br.com.spbank.administracao.adapter.out.persistence.mysql.data.AdministratorCredentialData;
import br.com.spbank.administracao.adapter.out.persistence.mysql.data.AdministratorSessionData;
import br.com.spbank.administracao.adapter.out.persistence.mysql.repository.AccountPlanChangeJpaRepository;
import br.com.spbank.administracao.adapter.out.persistence.mysql.repository.AdministratorCredentialJpaRepository;
import br.com.spbank.administracao.adapter.out.persistence.mysql.repository.AdministratorSessionJpaRepository;
import br.com.spbank.administracao.application.model.AccountPlanChange;
import br.com.spbank.administracao.application.model.AdministratorCredential;
import br.com.spbank.administracao.application.model.AdministratorSession;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public final class AdministrationMysqlPersistence
        implements AdministrationPersistence {

    private final AdministratorCredentialJpaRepository credentials;
    private final AdministratorSessionJpaRepository sessions;
    private final AccountPlanChangeJpaRepository planChanges;

    public AdministrationMysqlPersistence(
            AdministratorCredentialJpaRepository credentials,
            AdministratorSessionJpaRepository sessions,
            AccountPlanChangeJpaRepository planChanges
    ) {
        this.credentials = credentials;
        this.sessions = sessions;
        this.planChanges = planChanges;
    }

    @Override
    public Optional<AdministratorCredential> findCredential(
            String username
    ) {

        return credentials
                .findByUsernameIgnoreCase(username)
                .map(data ->
                        credential(data)
                );
    }

    @Override
    public Optional<AdministratorCredential> findCredentialById(
            UUID administratorId
    ) {

        return credentials
                .findById(administratorId)
                .map(data ->
                        credential(data)
                );
    }

    @Override
    public Optional<AdministratorSession> findSession(
            String tokenHash
    ) {

        return sessions
                .findByTokenHash(tokenHash)
                .map(data ->
                        new AdministratorSession(
                                data.getId(),
                                data.getAdministratorId(),
                                data.getTokenHash(),
                                data.getCreatedAt(),
                                data.getExpiresAt(),
                                data.getRevokedAt()
                        )
                );
    }

    @Override
    public void saveSession(
            AdministratorSession session
    ) {

        sessions.save(
                new AdministratorSessionData(
                        session.id(),
                        session.administratorId(),
                        session.tokenHash(),
                        session.createdAt(),
                        session.expiresAt(),
                        session.revokedAt()
                )
        );
    }

    @Override
    public void revokeSession(
            String tokenHash,
            Instant revokedAt
    ) {

        sessions.revoke(
                tokenHash,
                revokedAt
        );
    }

    @Override
    public void savePlanChange(
            AccountPlanChange change
    ) {

        planChanges.save(
                new AccountPlanChangeData(
                        change.id(),
                        change.accountId(),
                        change.administratorId(),
                        change.administratorName(),
                        change.previousPlan(),
                        change.newPlan(),
                        change.reason(),
                        change.changedAt()
                )
        );
    }

    @Override
    public List<AccountPlanChange> findPlanChanges(
            UUID accountId
    ) {

        return planChanges
                .findByAccountIdOrderByChangedAtDesc(
                        accountId
                )
                .stream()
                .map(data ->
                        new AccountPlanChange(
                                data.getId(),
                                data.getAccountId(),
                                data.getAdministratorId(),
                                data.getAdministratorName(),
                                data.getPreviousPlan(),
                                data.getNewPlan(),
                                data.getReason(),
                                data.getChangedAt()
                        )
                )
                .toList();
    }

    private static AdministratorCredential credential(
            AdministratorCredentialData data
    ) {

        return new AdministratorCredential(
                data.getId(),
                data.getDisplayName(),
                data.getUsername(),
                data.getPasswordHash(),
                data.isActive()
        );
    }
}