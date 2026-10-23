package br.com.spbank.pix.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.conta.application.model.EntryType;
import br.com.spbank.conta.application.port.out.AccountEntryPersistence;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.pix.application.model.PixDestination;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.model.PixStatus;
import br.com.spbank.pix.application.port.out.PixBusinessLogPort;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.port.out.PixSettlementPort;
import br.com.spbank.pix.application.service.PixProperties;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PixExecutionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-27T13:00:00Z");

    @Test
    void shouldDebitAndCreditExactlyOnceAndCreateTwoEntries() {
        UUID sourceId =
                UUID.fromString("11111111-1111-1111-1111-111111111111");

        UUID targetId =
                UUID.fromString("22222222-2222-2222-2222-222222222222");

        Account source = new Account(
                sourceId,
                UUID.randomUUID(),
                "Alexandre Souza",
                "52998224725",
                "90001",
                "0001",
                "100010",
                AccountType.CURRENT,
                new BigDecimal("500.00"),
                true
        );

        Account target = new Account(
                targetId,
                UUID.randomUUID(),
                "Roberta Silva",
                "11144477735",
                "90001",
                "0001",
                "200020",
                AccountType.CURRENT,
                new BigDecimal("100.00"),
                true
        );

        PixPayment payment = PixPayment.processing(
                sourceId,
                PixDestination.internal(
                        targetId,
                        "Roberta Silva",
                        "***7735",
                        "90001"
                ),
                PixKeyType.EMAIL,
                "r***@spbank.local",
                "a".repeat(64),
                new BigDecimal("75.00"),
                UUID.randomUUID(),
                "b".repeat(64),
                NOW
        );

        AccountPersistence accounts =
                java.util.Objects.requireNonNull(mock(AccountPersistence.class));

        AccountEntryPersistence entries =
                java.util.Objects.requireNonNull(mock(AccountEntryPersistence.class));

        PixPersistence persistence =
                java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(accounts.findAllForUpdate(
                List.of(sourceId, targetId)
        )).thenReturn(Map.of(
                sourceId, source,
                targetId, target
        ));

        when(persistence.savePayment(payment))
                .thenReturn(payment);

        var service = new PixExecutionService(
                accounts,
                entries,
                persistence,
                mock(PixSettlementPort.class),
                mock(PixBusinessLogPort.class),
                properties(),
                clock()
        );

        PixPayment result = service.executeLocked(payment);

        assertThat(result.getStatus())
                .isEqualTo(PixStatus.COMPLETED);

        assertThat(source.getBalance())
                .isEqualByComparingTo("425.00");

        assertThat(target.getBalance())
                .isEqualByComparingTo("175.00");

        verify(accounts).saveAll(
                argThat(values -> values.size() == 2)
        );

        verify(entries).saveAll(
                argThat(values ->
                        values.size() == 2
                        && values.stream().anyMatch(entry ->
                                entry.type() == EntryType.PIX_OUT
                        )
                        && values.stream().anyMatch(entry ->
                                entry.type() == EntryType.PIX_IN
                        )
                )
        );
    }

    @Test
    void shouldDebitOnlySourceAndSettleWhenPixIsExternal() {
        UUID sourceId =
                UUID.fromString("11111111-1111-1111-1111-111111111111");

        Account source = new Account(
                sourceId,
                UUID.randomUUID(),
                "Alexandre Souza",
                "52998224725",
                "90001",
                "0001",
                "100010",
                AccountType.CURRENT,
                new BigDecimal("500.00"),
                true
        );

        PixPayment payment = PixPayment.processing(
                sourceId,
                PixDestination.external(
                        "Conta de outra instituição",
                        "Não informado",
                        "EXTERNAL",
                        "Outra instituição"
                ),
                PixKeyType.EMAIL,
                "c***@banco.com.br",
                "a".repeat(64),
                new BigDecimal("75.00"),
                UUID.randomUUID(),
                "b".repeat(64),
                NOW
        );

        AccountPersistence accounts =
                java.util.Objects.requireNonNull(mock(AccountPersistence.class));

        AccountEntryPersistence entries =
                java.util.Objects.requireNonNull(mock(AccountEntryPersistence.class));

        PixPersistence persistence =
                java.util.Objects.requireNonNull(mock(PixPersistence.class));

        PixSettlementPort settlement =
                java.util.Objects.requireNonNull(mock(PixSettlementPort.class));

        when(accounts.findAllForUpdate(
                List.of(sourceId)
        )).thenReturn(Map.of(sourceId, source));

        when(settlement.settle(any()))
                .thenReturn("E9000100REFERENCE");

        when(persistence.savePayment(payment))
                .thenReturn(payment);

        var service = new PixExecutionService(
                accounts,
                entries,
                persistence,
                settlement,
                mock(PixBusinessLogPort.class),
                properties(),
                clock()
        );

        PixPayment result = service.executeLocked(payment);

        assertThat(result.getStatus())
                .isEqualTo(PixStatus.COMPLETED);

        assertThat(result.getSettlementReference())
                .isEqualTo("E9000100REFERENCE");

        assertThat(source.getBalance())
                .isEqualByComparingTo("425.00");

        verify(accounts).saveAll(
                argThat(values -> values.size() == 1)
        );

        verify(entries).saveAll(
                argThat(values ->
                        values.size() == 1
                        && values.iterator().next().type()
                                == EntryType.PIX_OUT
                )
        );

        verify(settlement).settle(
                argThat(instruction ->
                        instruction.sourceAccountId().equals(sourceId)
                        && instruction.amount().compareTo(
                                new BigDecimal("75.00")
                        ) == 0
                )
        );
    }

    private static PixProperties properties() {
        return new PixProperties(
                5000,
                new BigDecimal("50000.00"),
                new BigDecimal("1000.00"),
                "America/Sao_Paulo"
        );
    }

    private static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}