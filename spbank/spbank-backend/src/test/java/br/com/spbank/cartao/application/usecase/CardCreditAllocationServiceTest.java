package br.com.spbank.cartao.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.spbank.autenticacao.application.model.*;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CardCreditAllocationServiceTest {
    private final UUID customerId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-23T12:00:00Z");
    private static final String PASSWORD_HASH = PasswordHasher.hash("Senha@123");
    private final CardCreditPersistence credit = mock(CardCreditPersistence.class);
    private final CardCreditAllocationPersistence allocations = mock(CardCreditAllocationPersistence.class);
    private final AuthenticationPersistence customers = mock(AuthenticationPersistence.class);
    private final Map<UUID, CreditAllocationOperation> operations = new HashMap<>();
    private List<CardCreditLine> lines = List.of();
    private CustomerPlan plan = CustomerPlan.PLUS;
    private CardCreditAllocationService service;

    @BeforeEach
    void setUp() {
        service = new CardCreditAllocationService(credit, allocations, customers, Clock.fixed(now, ZoneOffset.UTC));
        when(customers.findCustomerById(customerId)).thenAnswer(i -> Optional.of(customer()));
        when(customers.findCustomerByIdForUpdate(customerId)).thenAnswer(i -> Optional.of(customer()));
        when(customers.findCredentialByCustomerId(customerId)).thenReturn(Optional.of(
            new AccessCredential(customerId, "cliente", PASSWORD_HASH, true)));
        when(credit.approvedTotal(customerId)).thenReturn(money("10000"));
        when(allocations.lines(customerId)).thenAnswer(i -> lines);
        doAnswer(i -> { lines = List.copyOf(i.getArgument(1)); return null; })
            .when(allocations).saveLines(eq(customerId), any());
        when(allocations.findOperation(eq(customerId), any())).thenAnswer(i -> Optional.ofNullable(operations.get(i.getArgument(1))));
        doAnswer(i -> { CreditAllocationOperation op = i.getArgument(0); operations.put(op.key(), op); return null; })
            .when(allocations).saveOperation(any());
    }

    @Test void missingLinesStartAtZeroWithoutAutomaticallyAllocatingApprovedCredit() {
        var result = service.allocation(customerId);
        assertThat(result.approvedTotal()).isEqualByComparingTo("10000");
        assertThat(result.lines()).hasSize(3);
        assertThat(result.allocatedTotal()).isZero();
        verify(allocations, never()).saveLines(any(), any());
    }

    @Test void replacesThreeLinesAndPreservesCommittedAmounts() {
        lines = List.of(new CardCreditLine(CardBrand.BRISA, money("4000"), money("500")));
        var result = distribute(UUID.randomUUID(), desired("2000", "1500", "500"));
        assertThat(result.credit().allocatedTotal()).isEqualByComparingTo("4000");
        assertThat(result.credit().unallocatedTotal()).isEqualByComparingTo("6000");
        assertThat(result.credit().lines().get(CardBrand.BRISA).committed()).isEqualByComparingTo("500");
        assertThat(result.occurredAt()).isEqualTo(now);
        assertThat(lines).hasSize(3);
        verify(customers).findCustomerByIdForUpdate(customerId);
        verify(credit, never()).saveApprovedTotal(any(), any());
    }

    @Test void cannotAllocateMoreThanApproved() {
        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> distribute(UUID.randomUUID(), desired("10000", "1", "0")));
            assertThat(failure).isInstanceOf(BusinessException.class);
            assertThat(failure).hasMessage("card.credit.exceeds-approved");
        }
        assertNoWrites();
    }

    @Test void downgradePreservesOldAllocationAndDebtUntilValidManualAdjustment() {
        lines = List.of(new CardCreditLine(CardBrand.BRISA, money("8000"), money("6000")));
        plan = CustomerPlan.STANDARD;
        var current = service.allocation(customerId);
        assertThat(current.requiresAdjustment(plan)).isTrue();
        assertThat(current.approvedTotal()).isEqualByComparingTo("10000");
        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> distribute(UUID.randomUUID(), desired("6000", "0", "0")));
            assertThat(failure).isInstanceOf(BusinessException.class);
            assertThat(failure).hasMessage("card.credit.exceeds-plan");
        }
        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> distribute(UUID.randomUUID(), desired("5000", "0", "0")));
            assertThat(failure).isInstanceOf(BusinessException.class);
            assertThat(failure).hasMessage("card.credit.below-committed");
        }
        assertNoWrites();
    }

    @Test void canAdjustToTheNewPlanWhenCommittedAmountsFit() {
        plan = CustomerPlan.STANDARD;
        lines = List.of(new CardCreditLine(CardBrand.BRISA, money("8000"), money("3000")));
        var result = distribute(UUID.randomUUID(), desired("4000", "500", "500"));
        assertThat(result.credit().requiresAdjustment(plan)).isFalse();
        assertThat(result.credit().approvedTotal()).isEqualByComparingTo("10000");
    }

    @Test void replayKeepsTheOriginalSnapshotDespiteLaterChangesAndNumericFormatting() {
        var key = UUID.randomUUID();
        var first = distribute(key, desired("2000", "1500", "500"));
        distribute(UUID.randomUUID(), desired("1000", "1000", "1000"));
        var reversed = new LinkedHashMap<CardBrand, BigDecimal>();
        reversed.put(CardBrand.ELU, new BigDecimal("500.0"));
        reversed.put(CardBrand.MISTERSHARD, new BigDecimal("1500"));
        reversed.put(CardBrand.BRISA, new BigDecimal("2000.00"));
        assertThat(distribute(key, reversed)).isEqualTo(first);
        assertThat(service.allocation(customerId).allocatedTotal()).isEqualByComparingTo("3000");
        verify(allocations, times(2)).saveLines(any(), any());
    }

    @Test void changingIntentionWithSameKeyConflicts() {
        var key = UUID.randomUUID();
        distribute(key, desired("2000", "1500", "500"));
        assertThatThrownBy(() -> distribute(key, desired("2001", "1500", "500")))
            .isInstanceOf(IdempotencyConflictException.class);
        assertThat(operations).hasSize(1);
    }

    @Test void replayStillRequiresTheCorrectPassword() {
        var key = UUID.randomUUID();
        var intent = desired("2000", "1500", "500");
        distribute(key, intent);
        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> service.redistribute(customerId, key, intent, "errada"));
            assertThat(failure).isInstanceOf(BusinessException.class);
            assertThat(failure).hasMessage("card.request.password");
        }
        verify(allocations, times(1)).saveLines(any(), any());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"errada"})
    void invalidPasswordDoesNotConsumeKeyOrChangeLines(String password) {
        assertThatThrownBy(() -> service.redistribute(customerId, UUID.randomUUID(), desired("1", "2", "3"), password))
            .isInstanceOf(BusinessException.class);
        assertNoWrites();
    }

    @ParameterizedTest @ValueSource(strings = {"-1", "1.001", "100000000000000000"})
    void invalidAmountsAreBusinessErrors(String value) {
        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> distribute(UUID.randomUUID(), desired(value, "0", "0")));
            assertThat(failure).isInstanceOf(BusinessException.class);
            assertThat(failure).hasMessage("card.credit.invalid-amount");
        }
        assertNoWrites();
    }

    @Test void incompleteAllocationIsRejected() {
        assertThatThrownBy(() -> distribute(UUID.randomUUID(), Map.of(CardBrand.BRISA, money("1"))))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> distribute(UUID.randomUUID(), null)).isInstanceOf(BusinessException.class);
        assertNoWrites();
    }

    @Test void anotherCustomerCannotAccessOrChangeThisAllocation() {
        var other = UUID.randomUUID();
        assertThatThrownBy(() -> service.allocation(other)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.redistribute(other, UUID.randomUUID(), desired("1", "2", "3"), "Senha@123"))
            .isInstanceOf(UnauthorizedException.class);
        assertNoWrites();
    }

    @Test void snapshotContainsNoPasswordAndFingerprintIsHashed() {
        distribute(UUID.randomUUID(), desired("1", "2", "3"));
        var operation = operations.values().iterator().next();
        assertThat(operation.fingerprint()).matches("[0-9a-f]{64}");
        assertThat(operation.toString()).doesNotContain("Senha@123", PASSWORD_HASH);
    }

    private void assertNoWrites() {
        verify(allocations, never()).saveLines(any(), any());
        verify(allocations, never()).saveOperation(any());
    }
    private CreditAllocationResult distribute(UUID key, Map<CardBrand, BigDecimal> intent) {
        return service.redistribute(customerId, key, intent, "Senha@123");
    }
    private static Map<CardBrand, BigDecimal> desired(String brisa, String mistershard, String elu) {
        return Map.of(CardBrand.BRISA, new BigDecimal(brisa), CardBrand.MISTERSHARD,
            new BigDecimal(mistershard), CardBrand.ELU, new BigDecimal(elu));
    }
    private Customer customer() {
        return new Customer(customerId, "Cliente", "52998224725", LocalDate.of(1990, 1, 1),
            "11987654321", "cliente@example.com", new CustomerAddress("01001000", "Rua", "1",
            null, "Centro", "Sao Paulo", "SP"), plan, true);
    }
    private static BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
