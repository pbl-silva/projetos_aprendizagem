package br.com.spbank.cartao.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.out.CardCreditPersistence;
import br.com.spbank.autenticacao.application.model.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.administracao.application.model.AdministratorCredential;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.shared.application.model.CustomerPlan;
import br.com.spbank.shared.application.exception.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CardCreditServiceTest {
    private final UUID customerId = UUID.randomUUID(), adminId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-14T12:00:00Z");
    private static final String PASSWORD_HASH = PasswordHasher.hash("Senha@123");
    private final Map<UUID, CreditRequest> requests = new LinkedHashMap<>();
    private final Map<UUID, CreditOperation> operations = new HashMap<>();
    private BigDecimal total = new BigDecimal("0.00");
    private CustomerPlan plan = CustomerPlan.PLUS;
    private final CardCreditPersistence credit = mock(CardCreditPersistence.class);
    private final AuthenticationPersistence customers = mock(AuthenticationPersistence.class);
    private final AdministrationPersistence administrators = mock(AdministrationPersistence.class);
    private CardCreditService service;

    @BeforeEach void setUp() {
        service = at(now);
        when(customers.findCustomerByIdForUpdate(customerId)).thenAnswer(i -> Optional.of(customer()));
        when(customers.findCustomerById(customerId)).thenAnswer(i -> Optional.of(customer()));
        when(customers.findCredentialByCustomerId(customerId)).thenReturn(Optional.of(
            new AccessCredential(customerId, "cliente", PASSWORD_HASH, true)));
        when(administrators.findCredentialById(adminId)).thenReturn(Optional.of(
            new AdministratorCredential(adminId, "Gerente", "gerente", "unused", true)));
        when(credit.approvedTotal(customerId)).thenAnswer(i -> total);
        doAnswer(i -> { total = i.getArgument(1); return null; })
            .when(credit).saveApprovedTotal(eq(customerId), any());
        when(credit.requests(customerId)).thenAnswer(i -> List.copyOf(requests.values()));
        when(credit.findRequest(eq(customerId), any())).thenAnswer(i -> Optional.ofNullable(requests.get(i.getArgument(1))));
        doAnswer(i -> { CreditRequest r = i.getArgument(0); requests.put(r.id(), r); return null; })
            .when(credit).saveRequest(any());
        when(credit.findOperation(eq(customerId), any())).thenAnswer(i -> Optional.ofNullable(operations.get(i.getArgument(1))));
        doAnswer(i -> { CreditOperation op = i.getArgument(0); operations.put(op.key(), op); return null; })
            .when(credit).saveOperation(any());
    }
    @Test void requestAndOfferDoNotGrantCreditButAcceptanceDoes() {
        var request = request("10000");
        assertThat(total).isZero();
        var offer = offer(request, "8000");
        assertThat(offer.request().expiresAt()).isEqualTo(now.plus(Duration.ofDays(7)));
        assertThat(total).isZero();
        var result = service.accept(customerId, request.id(), UUID.randomUUID(), "Senha@123");
        assertThat(result.request().status()).isEqualTo(CreditRequestStatus.ACCEPTED);
        assertThat(total).isEqualByComparingTo("8000");
        assertThat(operations).hasSize(3);
    }
    @Test void sameKeyReturnsOriginalSnapshotEvenAfterLaterChanges() {
        var key = UUID.randomUUID();
        var initial = service.request(customerId, key, money("10000"), "Credito inicial");
        offer(initial.request(), "8000");
        assertThat(service.request(customerId, key, money("10000.00"), "Credito inicial")).isEqualTo(initial);
        assertThat(initial.request().status()).isEqualTo(CreditRequestStatus.REQUESTED);
    }
    @Test void changedPayloadWithSameKeyConflicts() {
        var key = UUID.randomUUID();
        service.request(customerId, key, money("10000"), "Credito inicial");
        assertThatThrownBy(() -> service.request(customerId, key, money("9000"), "Credito inicial"))
            .isInstanceOf(IdempotencyConflictException.class);
    }
    @Test void cannotHaveTwoPendingRequests() {
        request("10000");
        assertThatThrownBy(() -> request("12000")).isInstanceOf(BusinessException.class);
        assertThat(requests).hasSize(1);
    }
    @Test void offerAlsoPreventsAnotherRequest() {
        offer(request("10000"), "8000");
        assertThatThrownBy(() -> request("12000")).isInstanceOf(BusinessException.class);
    }
    @Test void canCancelAndRequestAgainWithHistoryPreserved() {
        var old = request("10000");
        var key = UUID.randomUUID();
        var cancelled = service.cancel(customerId, old.id(), key);
        assertThat(service.cancel(customerId, old.id(), key)).isEqualTo(cancelled);
        request("12000");
        assertThat(requests.get(old.id()).status()).isEqualTo(CreditRequestStatus.CANCELLED);
        assertThat(requests).hasSize(2);
    }
    @Test void rejectRequiresReasonAndDoesNotGrantCredit() {
        var request = request("10000");
        var result = service.decide(adminId, customerId, request.id(), UUID.randomUUID(),
            CreditDecision.REJECT, null, "Analise recusada");
        assertThat(result.request().status()).isEqualTo(CreditRequestStatus.REJECTED);
        assertThat(total).isZero();
        request("9000");
    }
    @Test void offerCannotExceedRequestedTotal() {
        var request = request("5000");
        assertThatThrownBy(() -> offer(request, "6000")).isInstanceOf(BusinessException.class);
    }
    @Test void downgradeRevalidatesOfferAtAcceptanceWithoutErasingApprovedCredit() {
        var request = request("10000");
        offer(request, "8000");
        plan = CustomerPlan.STANDARD;
        assertThatThrownBy(() -> service.accept(customerId, request.id(), UUID.randomUUID(), "Senha@123"))
            .isInstanceOf(BusinessException.class);
        assertThat(total).isZero();
        total = money("9000");
        assertThat(service.overview(customerId).approvedTotal()).isEqualByComparingTo("9000");
    }
    @Test void wrongPasswordDoesNotConsumeTheOffer() {
        var request = request("10000"); offer(request, "8000");
        assertThatThrownBy(() -> service.accept(customerId, request.id(), UUID.randomUUID(), "errada"))
            .isInstanceOf(BusinessException.class);
        assertThat(total).isZero();
        assertThat(requests.get(request.id()).status()).isEqualTo(CreditRequestStatus.OFFERED);
    }
    @Test void acceptanceIsIdempotentButStillRequiresPasswordOnReplay() {
        var request = request("10000"); offer(request, "8000");
        var key = UUID.randomUUID();
        var first = service.accept(customerId, request.id(), key, "Senha@123");
        assertThat(service.accept(customerId, request.id(), key, "Senha@123")).isEqualTo(first);
        verify(credit, times(1)).saveApprovedTotal(customerId, money("8000"));
        assertThatThrownBy(() -> service.accept(customerId, request.id(), key, "errada"))
            .isInstanceOf(BusinessException.class);
    }
    @Test void managerDecisionIsIdempotentAndAudited() {
        var request = request("10000"); var key = UUID.randomUUID();
        var first = service.decide(adminId, customerId, request.id(), key,
            CreditDecision.OFFER, money("8000"), "Aprovado");
        assertThat(service.decide(adminId, customerId, request.id(), key,
            CreditDecision.OFFER, money("8000.00"), "Aprovado")).isEqualTo(first);
        assertThat(first.actorId()).isEqualTo(adminId);
        assertThat(operations).hasSize(2);
    }
    @Test void offerExpiresExactlyAtSevenDays() {
        var request = request("10000"); offer(request, "8000");
        var expired = at(now.plus(Duration.ofDays(7)));
        assertThatThrownBy(() -> expired.accept(customerId, request.id(), UUID.randomUUID(), "Senha@123"))
            .isInstanceOf(BusinessException.class);
        assertThat(expired.overview(customerId).requests().getFirst().status()).isEqualTo(CreditRequestStatus.EXPIRED);
        expired.request(customerId, UUID.randomUUID(), money("12000"), "Nova analise");
        assertThat(requests.get(request.id()).status()).isEqualTo(CreditRequestStatus.EXPIRED);
    }
    @Test void offerIsValidImmediatelyBeforeExpiry() {
        var request = request("10000"); offer(request, "8000");
        at(now.plus(Duration.ofDays(7)).minusNanos(1)).accept(customerId, request.id(), UUID.randomUUID(), "Senha@123");
        assertThat(total).isEqualByComparingTo("8000");
    }
    @Test void requestOfAnotherCustomerIsNotAccessible() {
        assertThatThrownBy(() -> service.cancel(customerId, UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(NotFoundException.class);
    }
    @Test void unknownManagerCannotDecide() {
        var request = request("10000");
        assertThatThrownBy(() -> service.decide(UUID.randomUUID(), customerId, request.id(),
            UUID.randomUUID(), CreditDecision.OFFER, money("8000"), "Aprovado"))
            .isInstanceOf(UnauthorizedException.class);
    }
    @ParameterizedTest @ValueSource(strings={"0", "-1", "20000.01", "1.001"})
    void invalidRequestedTotalsAreRejected(String amount) {
        assertThatThrownBy(() -> request(amount)).isInstanceOf(BusinessException.class);
    }
    @Test void requestIsANewTotalNotAnIncrement() {
        total = money("5000");
        assertThatThrownBy(() -> request("4000")).isInstanceOf(BusinessException.class);
        request("6000");
    }
    @Test void blankReasonsAreRejected() {
        assertThatThrownBy(() -> service.request(customerId, UUID.randomUUID(), money("1000"), " "))
            .isInstanceOf(BusinessException.class);
    }
    @Test void rejectionMustNotCarryAnOfferedTotal() {
        var r = request("1000");
        assertThatThrownBy(() -> service.decide(adminId, customerId, r.id(), UUID.randomUUID(),
            CreditDecision.REJECT, money("1000"), "Recusado")).isInstanceOf(BusinessException.class);
    }
    private CreditRequest request(String total) {
        return service.request(customerId, UUID.randomUUID(), money(total), "Credito inicial").request();
    }
    private CreditResult offer(CreditRequest request, String total) {
        return service.decide(adminId, customerId, request.id(), UUID.randomUUID(),
            CreditDecision.OFFER, money(total), "Analise aprovada");
    }
    private CardCreditService at(Instant instant) {
        return new CardCreditService(credit, customers, administrators, Clock.fixed(instant, ZoneOffset.UTC));
    }
    private Customer customer() {
        return new Customer(customerId, "Cliente", "52998224725", LocalDate.of(1990,1,1),
            "11987654321", "cliente@example.com", new CustomerAddress("01001000",
            "Rua", "1", null, "Centro", "Sao Paulo", "SP"), plan, true);
    }
    private static BigDecimal money(String value) { return new BigDecimal(value).setScale(value.contains(".") ? new BigDecimal(value).scale() : 2); }
}
