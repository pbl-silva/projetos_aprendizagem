package br.com.spbank.administracao.application.usecase;

import br.com.spbank.administracao.application.model.AccountPlanChange;
import br.com.spbank.administracao.application.model.AdministrativeAccount;
import br.com.spbank.administracao.application.model.AdministratorCredential;
import br.com.spbank.administracao.application.model.AdministratorLoginResult;
import br.com.spbank.administracao.application.model.AdministratorSession;
import br.com.spbank.administracao.application.model.AuthenticatedAdministrator;
import br.com.spbank.administracao.application.port.in.AdministrationUseCase;
import br.com.spbank.administracao.application.port.in.ChangeAccountPlanCommand;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.shared.application.exception.UnauthorizedException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministrationService
        implements AdministrationUseCase {

    private final AdministrationPersistence administration;
    private final AccountPersistence accounts;
    private final Clock clock;
    private final Duration sessionDuration;
    private final SecureRandom random = new SecureRandom();

    public AdministrationService(
            AdministrationPersistence administration,
            AccountPersistence accounts,
            Clock clock,
            @Value("${spbank.auth.session-hours:8}")
            long sessionHours
    ) {
        this.administration = administration;
        this.accounts = accounts;
        this.clock = clock;
        this.sessionDuration =
                Duration.ofHours(sessionHours);
    }

    @Override
    @Transactional
    public AdministratorLoginResult login(
            String username,
            String password
    ) {

        String normalized =
                Objects.requireNonNullElse(
                                username,
                                ""
                        )
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        AdministratorCredential credential =
                administration
                        .findCredential(normalized)
                        .filter(found ->
                                found != null
                                        && found.active()
                        )
                        .filter(found ->
                                PasswordHasher.matches(
                                        password,
                                        found.passwordHash()
                                )
                        )
                        .orElseThrow(
                                UnauthorizedException::new
                        );

        byte[] tokenBytes = new byte[32];

        random.nextBytes(
                tokenBytes
        );

        String token =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                tokenBytes
                        );

        Instant now =
                clock.instant();

        Instant expiresAt =
                now.plus(
                        sessionDuration
                );

        administration.saveSession(
                new AdministratorSession(
                        UUID.randomUUID(),
                        credential.id(),
                        PasswordHasher.tokenHash(
                                token
                        ),
                        now,
                        expiresAt,
                        null
                )
        );

        return new AdministratorLoginResult(
                token,
                expiresAt,
                credential.id(),
                credential.displayName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedAdministrator resolveSession(
            String accessToken
    ) {

        if (accessToken == null
                || accessToken.isBlank()) {

            throw new UnauthorizedException();
        }

        AdministratorSession session =
                administration
                        .findSession(
                                PasswordHasher.tokenHash(
                                        accessToken
                                )
                        )
                        .orElseThrow(
                                UnauthorizedException::new
                        );

        if (!session.isValidAt(
                clock.instant()
        )) {
            throw new UnauthorizedException();
        }

        AdministratorCredential credential =
                requireActiveAdministrator(
                        session.administratorId()
                );

        return new AuthenticatedAdministrator(
                credential.id(),
                credential.displayName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministrativeAccount> listAccounts(
            UUID administratorId
    ) {

        requireActiveAdministrator(
                administratorId
        );

        return accounts
                .findAll()
                .stream()
                .map(account ->
                        new AdministrativeAccount(
                                account.getId(),
                                account.getHolderName(),
                                maskDocument(
                                        account.getHolderDocument()
                                ),
                                account.getBranch(),
                                account.getAccountNumber(),
                                account.getAccountType(),
                                account.getAccountPlan(),
                                account.isActive()
                        )
                )
                .toList();
    }

    @Override
    @Transactional
    public AccountPlanChange changeAccountPlan(
            UUID administratorId,
            UUID accountId,
            ChangeAccountPlanCommand command
    ) {

        AdministratorCredential administrator =
                requireActiveAdministrator(
                        administratorId
                );

        Objects.requireNonNull(
                command,
                "command"
        );

        Objects.requireNonNull(
                command.accountPlan(),
                "accountPlan"
        );

        String reason =
                Objects.requireNonNullElse(
                                command.reason(),
                                ""
                        )
                        .trim();

        if (reason.isBlank()) {

            throw new BusinessException(
                    "ACCOUNT_PLAN_REASON_REQUIRED",
                    "admin.account-plan.reason-required"
            );
        }

        Account account =
                accounts
                        .findAllForUpdate(
                                List.of(accountId)
                        )
                        .get(accountId);

        if (account == null
                || !account.isActive()) {

            throw new NotFoundException(
                    "ACCOUNT_NOT_FOUND"
            );
        }

        if (account.getAccountPlan()
                == command.accountPlan()) {

            throw new BusinessException(
                    "ACCOUNT_PLAN_UNCHANGED",
                    "admin.account-plan.unchanged"
            );
        }

        var previous =
                account.getAccountPlan();

        account.changePlan(
                command.accountPlan()
        );

        accounts.saveAll(
                List.of(account)
        );

        AccountPlanChange change =
                new AccountPlanChange(
                        UUID.randomUUID(),
                        accountId,
                        administrator.id(),
                        administrator.displayName(),
                        previous,
                        command.accountPlan(),
                        reason,
                        clock.instant()
                );

        administration.savePlanChange(
                change
        );

        return change;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountPlanChange>
            listAccountPlanHistory(
                    UUID administratorId,
                    UUID accountId
            ) {

        requireActiveAdministrator(
                administratorId
        );

        if (accounts
                .findById(accountId)
                .isEmpty()) {

            throw new NotFoundException(
                    "ACCOUNT_NOT_FOUND"
            );
        }

        return administration
                .findPlanChanges(
                        accountId
                );
    }

    @Override
    @Transactional
    public void logout(
            String accessToken
    ) {

        if (accessToken != null
                && !accessToken.isBlank()) {

            administration.revokeSession(
                    PasswordHasher.tokenHash(
                            accessToken
                    ),
                    clock.instant()
            );
        }
    }

    private AdministratorCredential
            requireActiveAdministrator(
                    UUID administratorId
            ) {

        return administration
                .findCredentialById(
                        administratorId
                )
                .filter(found ->
                        found != null
                                && found.active()
                )
                .orElseThrow(
                        UnauthorizedException::new
                );
    }

    private static String maskDocument(
            String document
    ) {

        if (document == null
                || document.length() < 4) {

            return "***";
        }

        return "***"
                + document.substring(
                        document.length() - 4
                );
    }
}