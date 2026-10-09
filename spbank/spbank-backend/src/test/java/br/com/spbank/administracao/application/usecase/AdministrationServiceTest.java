package br.com.spbank.administracao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.spbank.administracao.application.model.*;
import br.com.spbank.administracao.application.port.in.ChangeCustomerPlanCommand;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.model.CustomerAddress;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.UnauthorizedException;
import br.com.spbank.shared.application.model.CustomerPlan;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdministrationServiceTest {

    private static final UUID ADMIN_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID CUSTOMER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final String ADMIN_HASH =
            "pbkdf2_sha256$120000$/2pRwqqLAmVJkHdO45ByvA==$K9aXI/0uRATy1Jc5YONPD9a3Y07R+weul7Lsj7Xv4n8=";

    private static final Instant NOW =
            Instant.parse("2026-09-02T15:00:00Z");

    private AdministrationPersistence administration;
    private AccountPersistence accounts;
    private AuthenticationPersistence customers;
    private AdministrationService service;

    @BeforeEach
    void setUp() {
        administration = mock(AdministrationPersistence.class);
        accounts = mock(AccountPersistence.class);
        customers = mock(AuthenticationPersistence.class);

        service = new AdministrationService(
                administration,
                accounts,
                customers,
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

        assertThat(result.administratorId()).isEqualTo(ADMIN_ID);
        assertThat(result.displayName()).isEqualTo("Gerente SPBank");
        assertThat(result.accessToken()).isNotBlank();

        assertThat(result.expiresAt())
                .isEqualTo(NOW.plus(Duration.ofHours(8)));

        ArgumentCaptor<AdministratorSession> session =
                ArgumentCaptor.forClass(AdministratorSession.class);

        verify(administration).saveSession(session.capture());

        assertThat(session.getValue().tokenHash())
                .doesNotContain(result.accessToken());
    }

    @Test
    void shouldRejectInvalidManagerCredentials() {
        when(administration.findCredential("gerente"))
                .thenReturn(Optional.of(administrator()));

        assertThatThrownBy(
                () -> service.login("gerente", "senha-incorreta")
        ).isInstanceOf(UnauthorizedException.class);

        verify(administration, never()).saveSession(any());
    }

    @Test
    void shouldResolveOnlyAnActiveAdministrativeSession() {
        String accessToken = "admin-access-token";
        String tokenHash = PasswordHasher.tokenHash(accessToken);

        when(administration.findSession(tokenHash))
                .thenReturn(Optional.of(
                        new AdministratorSession(
                                UUID.randomUUID(),
                                ADMIN_ID,
                                tokenHash,
                                NOW.minusSeconds(60),
                                NOW.plusSeconds(60),
                                null
                        )
                ));

        AuthenticatedAdministrator result =
                service.resolveSession(accessToken);

        assertThat(result.id()).isEqualTo(ADMIN_ID);
        assertThat(result.displayName()).isEqualTo("Gerente SPBank");
    }

    @Test
    void shouldPromoteAndAuditAStandardCustomer() {
        Customer customer = customer(CustomerPlan.STANDARD);

        when(customers.findCustomerByIdForUpdate(CUSTOMER_ID))
                .thenReturn(Optional.of(customer));

        CustomerPlanChange change = service.changeCustomerPlan(
                ADMIN_ID,
                CUSTOMER_ID,
                new ChangeCustomerPlanCommand(
                        CustomerPlan.PLUS,
                        "Critérios internos aprovados pelo gerente"
                )
        );

        verify(customers).saveCustomer(
                customer.withPlan(CustomerPlan.PLUS)
        );

        assertThat(change.previousPlan())
                .isEqualTo(CustomerPlan.STANDARD);

        assertThat(change.newPlan())
                .isEqualTo(CustomerPlan.PLUS);

        assertThat(change.administratorId())
                .isEqualTo(ADMIN_ID);

        assertThat(change.customerId())
                .isEqualTo(CUSTOMER_ID);

        verify(accounts, never()).saveAll(any());
        verify(administration).savePlanChange(change);
    }

    @Test
    void shouldAllowDowngradeAndKeepTheReason() {
        Customer customer = customer(CustomerPlan.PLUS);

        when(customers.findCustomerByIdForUpdate(CUSTOMER_ID))
                .thenReturn(Optional.of(customer));

        CustomerPlanChange change = service.changeCustomerPlan(
                ADMIN_ID,
                CUSTOMER_ID,
                new ChangeCustomerPlanCommand(
                        CustomerPlan.STANDARD,
                        "Cliente deixou de atender aos critérios do PLUS"
                )
        );

        verify(customers).saveCustomer(
                customer.withPlan(CustomerPlan.STANDARD)
        );

        assertThat(change.newPlan())
                .isEqualTo(CustomerPlan.STANDARD);

        assertThat(change.reason())
                .isEqualTo("Cliente deixou de atender aos critérios do PLUS");
    }

    @Test
    void shouldRejectAnUnchangedPlan() {
        Customer customer = customer(CustomerPlan.STANDARD);

        when(customers.findCustomerByIdForUpdate(CUSTOMER_ID))
                .thenReturn(Optional.of(customer));

        assertThatThrownBy(
                () -> service.changeCustomerPlan(
                        ADMIN_ID,
                        CUSTOMER_ID,
                        new ChangeCustomerPlanCommand(
                                CustomerPlan.STANDARD,
                                "Sem mudança"
                        )
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting(error ->
                        ((BusinessException) error).getCode()
                )
                .isEqualTo("CUSTOMER_PLAN_UNCHANGED");

        verify(administration, never()).savePlanChange(any());
    }

    @Test
    void shouldRejectAPlanChangeWithoutReason() {
        assertThatThrownBy(
                () -> service.changeCustomerPlan(
                        ADMIN_ID,
                        CUSTOMER_ID,
                        new ChangeCustomerPlanCommand(
                                CustomerPlan.PLUS,
                                "   "
                        )
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting(error ->
                        ((BusinessException) error).getCode()
                )
                .isEqualTo("CUSTOMER_PLAN_REASON_REQUIRED");

        verifyNoInteractions(accounts);
        verify(administration, never()).savePlanChange(any());
    }

    @Test
    void shouldListOneCustomerWithBothAccountsAndOnePlan() {
        when(customers.findAllCustomers())
                .thenReturn(List.of(customer(CustomerPlan.PLUS)));

        when(accounts.findAll())
                .thenReturn(List.of(
                        account(AccountType.CURRENT),
                        account(AccountType.SAVINGS)
                ));

        var result = service.listCustomers(ADMIN_ID);

        assertThat(result).hasSize(1);

        assertThat(result.getFirst().customerId())
                .isEqualTo(CUSTOMER_ID);

        assertThat(result.getFirst().customerPlan())
                .isEqualTo(CustomerPlan.PLUS);

        assertThat(result.getFirst().accounts())
                .hasSize(2);
    }

    @Test
    void shouldReadHistoryByCustomerNotAccount() {
        when(customers.findCustomerById(CUSTOMER_ID))
                .thenReturn(Optional.of(customer(CustomerPlan.PLUS)));

        CustomerPlanChange change = new CustomerPlanChange(
                UUID.randomUUID(),
                CUSTOMER_ID,
                ADMIN_ID,
                "Gerente SPBank",
                CustomerPlan.STANDARD,
                CustomerPlan.PLUS,
                "Aprovado",
                NOW
        );

        when(administration.findPlanChanges(CUSTOMER_ID))
                .thenReturn(List.of(change));

        assertThat(
                service.listCustomerPlanHistory(ADMIN_ID, CUSTOMER_ID)
        ).containsExactly(change);

        verifyNoInteractions(accounts);
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

    private static Customer customer(CustomerPlan plan) {
        return new Customer(
                CUSTOMER_ID,
                "Roberta Lima",
                "12345678909",
                LocalDate.of(1992, 9, 10),
                "21991234567",
                "roberta@spbank.local",
                new CustomerAddress(
                        "20040002",
                        "Rua da Assembleia",
                        "200",
                        null,
                        "Centro",
                        "Rio de Janeiro",
                        "RJ"
                ),
                plan,
                true
        );
    }

    private static Account account(AccountType type) {
        return new Account(
                UUID.randomUUID(),
                CUSTOMER_ID,
                "Roberta Lima",
                "12345678909",
                "90001",
                "0001",
                "123456",
                type,
                BigDecimal.ZERO,
                true
        );
    }
}