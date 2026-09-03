package br.com.spbank.administracao.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.spbank.administracao.application.model.*;
import br.com.spbank.administracao.application.port.in.ChangeAccountPlanCommand;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.UnauthorizedException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdministrationServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString(
        "cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID ACCOUNT_ID = UUID.fromString(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final String ADMIN_HASH =
        "pbkdf2_sha256$120000$/2pRwqqLAmVJkHdO45ByvA==$K9aXI/0uRATy1Jc5YONPD9a3Y07R+weul7Lsj7Xv4n8=";

    private static final Instant NOW =
        Instant.parse("2026-09-02T15:00:00Z");

    private AdministrationPersistence administration;
    private AccountPersistence accounts;
    private AdministrationService service;

    @BeforeEach
    void setUp() {
        administration = mock(AdministrationPersistence.class);
        accounts = mock(AccountPersistence.class);

        service = new AdministrationService(
            administration,
            accounts,
            Clock.fixed(NOW, ZoneOffset.UTC),
            8
        );

        when(administration.findCredentialById(ADMIN_ID))
            .thenReturn(Optional.of(administrator()));
    }

    @Test
    void shouldAuthenticateManagerAndCreateAnIndependentSession() {
        when(administration.findCredential("gerente"))
            .thenReturn(Optional.of(administrator()));

        AdministratorLoginResult result = service.login(
            " GERENTE ",
            "SPBankAdmin@123"
        );

        assertThat(result.administratorId())
            .isEqualTo(ADMIN_ID);

        assertThat(result.displayName())
            .isEqualTo("Gerente SPBank");

        assertThat(result.accessToken())
            .isNotBlank();

        assertThat(result.expiresAt())
            .isEqualTo(
                NOW.plus(Duration.ofHours(8))
            );

        ArgumentCaptor<AdministratorSession> session =
            ArgumentCaptor.forClass(
                AdministratorSession.class
            );

        verify(administration)
            .saveSession(session.capture());

        assertThat(session.getValue().tokenHash())
            .doesNotContain(result.accessToken());
    }

    @Test
    void shouldRejectInvalidManagerCredentials() {
        when(administration.findCredential("gerente"))
            .thenReturn(Optional.of(administrator()));

        assertThatThrownBy(() ->
            service.login(
                "gerente",
                "senha-incorreta"
            )
        )
            .isInstanceOf(
                UnauthorizedException.class
            );

        verify(administration, never())
            .saveSession(any());
    }

    @Test
    void shouldResolveOnlyAnActiveAdministrativeSession() {
        String accessToken =
            "admin-access-token";

        String tokenHash =
            br.com.spbank.autenticacao.application.service
                .PasswordHasher.tokenHash(accessToken);

        when(administration.findSession(tokenHash))
            .thenReturn(
                Optional.of(
                    new AdministratorSession(
                        UUID.randomUUID(),
                        ADMIN_ID,
                        tokenHash,
                        NOW.minusSeconds(60),
                        NOW.plusSeconds(60),
                        null
                    )
                )
            );

        AuthenticatedAdministrator result =
            service.resolveSession(accessToken);

        assertThat(result.id())
            .isEqualTo(ADMIN_ID);

        assertThat(result.displayName())
            .isEqualTo("Gerente SPBank");
    }

    @Test
    void shouldPromoteAndAuditAStandardAccount() {
        Account account =
            account(AccountPlan.STANDARD);

        when(
            accounts.findAllForUpdate(
                List.of(ACCOUNT_ID)
            )
        )
            .thenReturn(
                Map.of(
                    ACCOUNT_ID,
                    account
                )
            );

        AccountPlanChange change =
            service.changeAccountPlan(
                ADMIN_ID,
                ACCOUNT_ID,
                new ChangeAccountPlanCommand(
                    AccountPlan.PLUS,
                    "Critérios internos aprovados pelo gerente"
                )
            );

        assertThat(account.getAccountPlan())
            .isEqualTo(AccountPlan.PLUS);

        assertThat(change.previousPlan())
            .isEqualTo(AccountPlan.STANDARD);

        assertThat(change.newPlan())
            .isEqualTo(AccountPlan.PLUS);

        assertThat(change.administratorId())
            .isEqualTo(ADMIN_ID);

        verify(accounts)
            .saveAll(List.of(account));

        verify(administration)
            .savePlanChange(change);
    }

    @Test
    void shouldAllowDowngradeAndKeepTheReason() {
        Account account =
            account(AccountPlan.PLUS);

        when(
            accounts.findAllForUpdate(
                List.of(ACCOUNT_ID)
            )
        )
            .thenReturn(
                Map.of(
                    ACCOUNT_ID,
                    account
                )
            );

        AccountPlanChange change =
            service.changeAccountPlan(
                ADMIN_ID,
                ACCOUNT_ID,
                new ChangeAccountPlanCommand(
                    AccountPlan.STANDARD,
                    "Conta deixou de atender aos critérios do PLUS"
                )
            );

        assertThat(change.newPlan())
            .isEqualTo(AccountPlan.STANDARD);

        assertThat(change.reason())
            .isEqualTo(
                "Conta deixou de atender aos critérios do PLUS"
            );
    }

    @Test
    void shouldRejectAnUnchangedPlan() {
        Account account =
            account(AccountPlan.STANDARD);

        when(
            accounts.findAllForUpdate(
                List.of(ACCOUNT_ID)
            )
        )
            .thenReturn(
                Map.of(
                    ACCOUNT_ID,
                    account
                )
            );

        assertThatThrownBy(() ->
            service.changeAccountPlan(
                ADMIN_ID,
                ACCOUNT_ID,
                new ChangeAccountPlanCommand(
                    AccountPlan.STANDARD,
                    "Sem mudança"
                )
            )
        )
            .isInstanceOf(
                BusinessException.class
            )
            .extracting(error ->
                ((BusinessException) error)
                    .getCode()
            )
            .isEqualTo(
                "ACCOUNT_PLAN_UNCHANGED"
            );

        verify(administration, never())
            .savePlanChange(any());
    }

    @Test
    void shouldRejectAPlanChangeWithoutReason() {
        assertThatThrownBy(() ->
            service.changeAccountPlan(
                ADMIN_ID,
                ACCOUNT_ID,
                new ChangeAccountPlanCommand(
                    AccountPlan.PLUS,
                    "   "
                )
            )
        )
            .isInstanceOf(
                BusinessException.class
            )
            .extracting(error ->
                ((BusinessException) error)
                    .getCode()
            )
            .isEqualTo(
                "ACCOUNT_PLAN_REASON_REQUIRED"
            );

        verifyNoInteractions(accounts);

        verify(administration, never())
            .savePlanChange(any());
    }

    private static AdministratorCredential administrator() {
        return new AdministratorCredential(
            ADMIN_ID,
            "Gerente SPBank",
            "gerente",
            ADMIN_HASH,
            true
        );
    }

    private static Account account(
            AccountPlan plan
    ) {
        return new Account(
            ACCOUNT_ID,
            UUID.randomUUID(),
            "Roberta Lima",
            "12345678909",
            "001",
            "0001",
            "123456-7",
            AccountType.CURRENT,
            plan,
            new BigDecimal("2500.00"),
            true
        );
    }
}