package br.com.spbank.cartao.adapter.out.persistence.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CardCreditLineData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.data.CreditAllocationOperationData;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CardCreditLineJpaRepository;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.CreditAllocationOperationJpaRepository;
import br.com.spbank.cartao.application.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class CardCreditAllocationMysqlPersistenceTest {
    @Test void jsonRoundTripPreservesSnapshotWithEnumKeysAndDates() {
        var lines = java.util.Objects.requireNonNull(mock(CardCreditLineJpaRepository.class));
        var operations = java.util.Objects.requireNonNull(mock(CreditAllocationOperationJpaRepository.class));
        var adapter = new CardCreditAllocationMysqlPersistence(lines, operations, JsonMapper.builder().build());
        var customerId = UUID.randomUUID();
        var key = UUID.randomUUID();
        var result = new CreditAllocationResult(UUID.randomUUID(), CustomerCardCredit.initial(customerId),
            Instant.parse("2026-10-23T12:00:00Z"));
        var original = new CreditAllocationOperation(customerId, key, "a".repeat(64), result);
        adapter.saveOperation(original);
        ArgumentCaptor<@org.jspecify.annotations.Nullable CreditAllocationOperationData> stored = ArgumentCaptor.forClass(CreditAllocationOperationData.class);
        java.util.Objects.requireNonNull(verify(operations)).save(br.com.spbank.MockitoArguments.nonNullMarker(stored.capture()));
        when(operations.findByCustomerIdAndKey(customerId, key)).thenReturn(Optional.of(java.util.Objects.requireNonNull(stored.getValue())));
        assertThat(adapter.findOperation(customerId, key)).contains(original);
    }

    @Test void readsOnlyCustomerLinesAndPersistsCommittedValueUnchanged() {
        var lines = java.util.Objects.requireNonNull(mock(CardCreditLineJpaRepository.class));
        var operations = java.util.Objects.requireNonNull(mock(CreditAllocationOperationJpaRepository.class));
        var adapter = new CardCreditAllocationMysqlPersistence(lines, operations, JsonMapper.builder().build());
        var customerId = UUID.randomUUID();
        when(lines.findByCustomerId(customerId)).thenReturn(List.of(new CardCreditLineData(
            customerId, CardBrand.BRISA, new BigDecimal("100.00"), new BigDecimal("120.00"))));
        var result = adapter.lines(customerId);
        assertThat(result.getFirst().excess()).isEqualByComparingTo("20");
        adapter.saveLines(customerId, result);
        java.util.Objects.requireNonNull(verify(lines)).saveAll(br.com.spbank.MockitoArguments.nonNullMarker(argThat(values -> {
            var value = java.util.Objects.requireNonNull(values).iterator().next();
            return value.getCustomerId().equals(customerId)
                && value.getCommitted().compareTo(new BigDecimal("120")) == 0;
        })));
    }
}
