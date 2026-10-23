package br.com.spbank.pix.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.conta.application.port.out.BankInstitutionCatalog;
import br.com.spbank.pix.application.model.*;
import br.com.spbank.pix.application.port.in.PixCreationCommand;
import br.com.spbank.pix.application.port.in.PixFavoriteRegistrationCommand;
import br.com.spbank.pix.application.port.in.PixPreview;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.service.PixProperties;
import br.com.spbank.shared.application.exception.BusinessException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.Test;

class PixServiceTest {

    @Test
    void shouldDeriveCpfKeyFromAuthenticatedAccountInsteadOfRequestValue() {
        UUID accountId = UUID.randomUUID();

        Account source = account(
                accountId, "Alexandre Souza", "52998224725", "500.00"
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(accounts.findById(accountId))
                .thenReturn(Optional.of(source));

        when(persistence.saveKey(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = service(
                accounts,
                persistence,
                mock(PixExecutionService.class),
                mock(BankInstitutionCatalog.class)
        );

        PixKey registered = service.registerKey(
                accountId,
                PixKeyType.CPF_CNPJ,
                "11144477735"
        );

        assertThat(registered.getNormalizedValue())
                .isEqualTo("52998224725");

        verify(persistence).findActiveKey(
                PixKeyType.CPF_CNPJ,
                "52998224725"
        );

        verify(persistence, never()).findActiveKey(
                PixKeyType.CPF_CNPJ,
                "11144477735"
        );
    }

    @Test
    void shouldExecutePixImmediatelyWhenScheduledDateIsToday() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Account source = account(
                sourceId, "Alexandre Souza", "52998224725", "500.00"
        );

        Account target = account(
                targetId, "Roberta Silva", "11144477735", "100.00"
        );

        PixKey key = new PixKey(
                UUID.randomUUID(),
                targetId,
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                true,
                Instant.parse("2026-08-20T12:00:00Z")
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));
        PixExecutionService execution = java.util.Objects.requireNonNull(mock(PixExecutionService.class));

        when(accounts.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(accounts.findById(targetId))
                .thenReturn(Optional.of(target));

        when(accounts.findAllForUpdate(List.of(sourceId, targetId)))
                .thenReturn(Map.of(
                        sourceId, source,
                        targetId, target
                ));

        when(persistence.findActiveKey(
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.of(key));

        when(persistence.findBySourceAndIdempotencyKey(any(), any()))
                .thenReturn(Optional.empty());

        when(persistence.savePayment(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(execution.executeLocked(any())).thenAnswer(invocation -> {
            PixPayment payment = invocation.getArgument(0);
            payment.complete(
                    Instant.parse("2026-08-27T13:00:01Z"),
                    null
            );
            return payment;
        });

        var service = service(
                accounts,
                persistence,
                execution,
                mock(BankInstitutionCatalog.class)
        );

        PixCreationCommand command = new PixCreationCommand(
                sourceId,
                UUID.randomUUID(),
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                new BigDecimal("25.00"),
                LocalDate.of(2026, 8, 27),
                null,
                null,
                false
        );

        PixPayment result = service.create(command);

        assertThat(result.getStatus())
                .isEqualTo(PixStatus.COMPLETED);

        assertThat(result.getScheduledFor()).isNull();

        verify(execution).executeLocked(any());
    }

    @Test
    void shouldCreateMonthlySeriesWithoutMovingBalanceBeforeDueDate() {
        UUID sourceId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        UUID targetId = UUID.fromString(
                "22222222-2222-2222-2222-222222222222"
        );

        Account source = account(
                sourceId, "Alexandre Souza", "52998224725", "500.00"
        );

        Account target = account(
                targetId, "Roberta Silva", "11144477735", "100.00"
        );

        PixKey key = new PixKey(
                UUID.randomUUID(),
                targetId,
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                true,
                Instant.parse("2026-08-20T12:00:00Z")
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));
        PixExecutionService execution = java.util.Objects.requireNonNull(mock(PixExecutionService.class));

        when(accounts.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(accounts.findById(targetId))
                .thenReturn(Optional.of(target));

        when(accounts.findAllForUpdate(List.of(sourceId, targetId)))
                .thenReturn(Map.of(
                        sourceId, source,
                        targetId, target
                ));

        when(persistence.findActiveKey(
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.of(key));

        when(persistence.findBySourceAndIdempotencyKey(any(), any()))
                .thenReturn(Optional.empty());

        when(persistence.savePayments(any()))
                .thenAnswer(invocation ->
                        new ArrayList<PixPayment>(
                                invocation.getArgument(0)
                        )
                );

        var service = service(
                accounts,
                persistence,
                execution,
                mock(BankInstitutionCatalog.class)
        );

        PixCreationCommand command = new PixCreationCommand(
                sourceId,
                UUID.randomUUID(),
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                new BigDecimal("25.00"),
                LocalDate.of(2026, 9, 30),
                PixFrequency.MONTHLY,
                3,
                false
        );

        PixPayment first = service.create(command);

        assertThat(first.getStatus())
                .isEqualTo(PixStatus.SCHEDULED);

        assertThat(first.getScheduledFor())
                .isEqualTo(LocalDate.of(2026, 9, 30));

        assertThat(source.getBalance())
                .isEqualByComparingTo("500.00");

        assertThat(target.getBalance())
                .isEqualByComparingTo("100.00");

        verify(persistence).savePayments(argThat(series -> {
            List<LocalDate> dates = series.stream()
                    .map(row -> java.util.Objects.requireNonNull(row).getScheduledFor())
                    .toList();

            return dates.equals(List.of(
                    LocalDate.of(2026, 9, 30),
                    LocalDate.of(2026, 10, 30),
                    LocalDate.of(2026, 11, 30)
            ));
        }));

        verifyNoInteractions(execution);
    }

    @Test
    void shouldAllowPixAtLimitAndRequireFavoriteAboveIt() {
        UUID sourceId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        Account source = account(
                sourceId, "Alexandre Souza", "52998224725", "5000.00"
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(accounts.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(persistence.findActiveKey(
                PixKeyType.EMAIL,
                "cliente@banco.com.br"
        )).thenReturn(Optional.empty());

        when(persistence.findUsableFavorite(
                sourceId,
                PixKeyType.EMAIL,
                "cliente@banco.com.br"
        )).thenReturn(Optional.empty());

        var service = service(
                accounts,
                persistence,
                mock(PixExecutionService.class),
                mock(BankInstitutionCatalog.class)
        );

        PixPreview preview = service.preview(
                command(sourceId, "1000.00")
        );

        assertThat(preview.destinationBankName())
                .isEqualTo("Outra instituição");

        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> service.preview(command(sourceId, "1000.01")));
            assertThat(failure).isInstanceOf(BusinessException.class);
            var businessFailure=(BusinessException) java.util.Objects.requireNonNull(failure);
            assertThat(businessFailure.getCode()).isEqualTo("PIX_FAVORITE_REQUIRED");
        }
    }

    @Test
    void shouldAlsoRequireFavoriteForHighInternalPix() {
        UUID sourceId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        UUID targetId = UUID.fromString(
                "22222222-2222-2222-2222-222222222222"
        );

        Account source = account(
                sourceId, "Alexandre Souza", "52998224725", "5000.00"
        );

        Account target = account(
                targetId, "Roberta Silva", "11144477735", "1000.00"
        );

        PixKey key = new PixKey(
                UUID.randomUUID(),
                targetId,
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                true,
                Instant.parse("2026-08-20T12:00:00Z")
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(accounts.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(accounts.findById(targetId))
                .thenReturn(Optional.of(target));

        when(persistence.findActiveKey(
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.of(key));

        when(persistence.findUsableFavorite(
                sourceId,
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.empty());

        var service = service(
                accounts,
                persistence,
                mock(PixExecutionService.class),
                mock(BankInstitutionCatalog.class)
        );

        PixCreationCommand highInternal = new PixCreationCommand(
                sourceId,
                null,
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                new BigDecimal("1000.01"),
                null,
                null,
                null,
                false
        );

        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> service.preview(highInternal));
            assertThat(failure).isInstanceOf(BusinessException.class);
            var businessFailure=(BusinessException) java.util.Objects.requireNonNull(failure);
            assertThat(businessFailure.getCode()).isEqualTo("PIX_FAVORITE_REQUIRED");
        }

        PixFavorite favorite = PixFavorite.internal(
                sourceId,
                key.getId(),
                "Roberta Silva",
                "roberta@spbank.local",
                "r***@spbank.local",
                PixKeyType.EMAIL,
                "***7735",
                Instant.parse("2026-08-20T12:00:00Z")
        );

        when(persistence.findUsableFavorite(
                sourceId,
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.of(favorite));

        assertThat(service.preview(highInternal).recipientName())
                .isEqualTo("Roberta Silva");
    }

    @Test
    void shouldRegisterInternalFavoriteUsingSelectedCatalogBank() {
        UUID sourceId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        UUID targetId = UUID.fromString(
                "22222222-2222-2222-2222-222222222222"
        );

        Account source = account(
                sourceId, "Alexandre Souza", "52998224725", "5000.00"
        );

        Account target = account(
                targetId, "Roberta Silva", "11144477735", "1000.00"
        );

        PixKey key = new PixKey(
                UUID.randomUUID(),
                targetId,
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                true,
                Instant.parse("2026-08-20T12:00:00Z")
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(accounts.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(accounts.findById(targetId))
                .thenReturn(Optional.of(target));

        when(persistence.findFavorite(
                sourceId,
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.empty());

        when(persistence.findActiveKey(
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.of(key));

        when(persistence.saveFavorite(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankInstitutionCatalog banks =
                java.util.Objects.requireNonNull(mock(BankInstitutionCatalog.class));

        when(banks.findActiveByCode("90001"))
                .thenReturn(Optional.of(
                        new BankInstitution(
                                "90001",
                                "SPBank",
                                true,
                                true
                        )
                ));

        var service = service(
                accounts,
                persistence,
                mock(PixExecutionService.class),
                banks
        );

        PixFavorite favorite = service.registerFavorite(
                new PixFavoriteRegistrationCommand(
                        sourceId,
                        PixKeyType.EMAIL,
                        "roberta@spbank.local",
                        null,
                        "90001",
                        null
                )
        );

        assertThat(favorite.routing())
                .isEqualTo(PixRouting.INTERNAL);

        assertThat(favorite.recipientName())
                .isEqualTo("Roberta Silva");

        assertThat(favorite.bankName())
                .isEqualTo("SPBank");
    }

    @Test
    void shouldAllowHighExternalPixForPreRegisteredFavorite() {
        UUID sourceId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        Account source = account(
                sourceId, "Alexandre Souza", "52998224725", "5000.00"
        );

        PixFavorite favorite = PixFavorite.external(
                sourceId,
                "Cliente Externo",
                "cliente@banco.com.br",
                "c***@banco.com.br",
                PixKeyType.EMAIL,
                "90002",
                "Banco Horizonte",
                "Não informado",
                Instant.parse("2026-08-20T12:00:00Z")
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(accounts.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(persistence.findActiveKey(
                PixKeyType.EMAIL,
                "cliente@banco.com.br"
        )).thenReturn(Optional.empty());

        when(persistence.findUsableFavorite(
                sourceId,
                PixKeyType.EMAIL,
                "cliente@banco.com.br"
        )).thenReturn(Optional.of(favorite));

        var service = service(
                accounts,
                persistence,
                mock(PixExecutionService.class),
                mock(BankInstitutionCatalog.class)
        );

        PixPreview preview = service.preview(
                command(sourceId, "1000.01")
        );

        assertThat(preview.recipientName())
                .isEqualTo("Cliente Externo");

        assertThat(preview.destinationBankCode())
                .isEqualTo("90002");
    }

    @Test
    void shouldNotAuthorizeHighPixWithInactiveInternalFavorite() {
        UUID sourceId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        Account source = account(
                sourceId, "Alexandre Souza", "52998224725", "5000.00"
        );

        AccountPersistence accounts = java.util.Objects.requireNonNull(mock(AccountPersistence.class));
        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(accounts.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(persistence.findActiveKey(
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.empty());

        when(persistence.findUsableFavorite(
                sourceId,
                PixKeyType.EMAIL,
                "roberta@spbank.local"
        )).thenReturn(Optional.empty());

        var service = service(
                accounts,
                persistence,
                mock(PixExecutionService.class),
                mock(BankInstitutionCatalog.class)
        );

        PixCreationCommand command = new PixCreationCommand(
                sourceId,
                null,
                PixKeyType.EMAIL,
                "roberta@spbank.local",
                new BigDecimal("1000.01"),
                null,
                null,
                null,
                false
        );

        {
            var failure=org.assertj.core.api.Assertions.catchThrowable(() -> service.preview(command));
            assertThat(failure).isInstanceOf(BusinessException.class);
            var businessFailure=(BusinessException) java.util.Objects.requireNonNull(failure);
            assertThat(businessFailure.getCode()).isEqualTo("PIX_FAVORITE_REQUIRED");
        }
    }

    @Test
    void shouldRemoveFavoritesLinkedToDeactivatedKey() {
        UUID sourceId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        UUID keyId = UUID.fromString(
                "33333333-3333-3333-3333-333333333333"
        );

        PixKey key = new PixKey(
                keyId,
                sourceId,
                PixKeyType.EMAIL,
                "alexandre@spbank.local",
                true,
                Instant.parse("2026-08-20T12:00:00Z")
        );

        PixPersistence persistence = java.util.Objects.requireNonNull(mock(PixPersistence.class));

        when(persistence.findKey(keyId, sourceId))
                .thenReturn(Optional.of(key));

        when(persistence.saveKey(key))
                .thenReturn(key);

        var service = service(
                mock(AccountPersistence.class),
                persistence,
                mock(PixExecutionService.class),
                mock(BankInstitutionCatalog.class)
        );

        service.deactivateKey(sourceId, keyId);

        assertThat(key.isActive()).isFalse();

        verify(persistence).deleteFavoritesByPixKeyId(keyId);
    }

    private static PixService service(
            AccountPersistence accounts,
            PixPersistence persistence,
            PixExecutionService execution,
            BankInstitutionCatalog banks
    ) {
        return new PixService(
                accounts,
                persistence,
                execution,
                banks,
                new PixProperties(
                        5000,
                        new BigDecimal("50000.00"),
                        new BigDecimal("1000.00"),
                        "America/Sao_Paulo"
                ),
                Clock.fixed(
                        Instant.parse("2026-08-27T13:00:00Z"),
                        ZoneOffset.UTC
                )
        );
    }

    private static PixCreationCommand command(
            UUID sourceId,
            String amount
    ) {
        return new PixCreationCommand(
                sourceId,
                null,
                PixKeyType.EMAIL,
                "cliente@banco.com.br",
                new BigDecimal(amount),
                null,
                null,
                null,
                false
        );
    }

    private static Account account(
            UUID id,
            String name,
            String document,
            String balance
    ) {
        return new Account(
                id,
                UUID.randomUUID(),
                name,
                document,
                "90001",
                "0001",
                id.toString().substring(0, 6),
                AccountType.CURRENT,
                new BigDecimal(balance),
                true
        );
    }
}
