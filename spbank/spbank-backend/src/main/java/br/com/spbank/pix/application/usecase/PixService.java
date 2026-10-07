package br.com.spbank.pix.application.usecase;

import br.com.spbank.conta.application.exception.AccountInactiveException;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.BankInstitution;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.conta.application.port.out.BankInstitutionCatalog;
import br.com.spbank.pix.application.model.PixDestination;
import br.com.spbank.pix.application.model.PixFavorite;
import br.com.spbank.pix.application.model.PixFrequency;
import br.com.spbank.pix.application.model.PixKey;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.model.PixRouting;
import br.com.spbank.pix.application.port.in.PixCreationCommand;
import br.com.spbank.pix.application.port.in.PixFavoriteRegistrationCommand;
import br.com.spbank.pix.application.port.in.PixFavoriteUseCase;
import br.com.spbank.pix.application.port.in.PixKeyUseCase;
import br.com.spbank.pix.application.port.in.PixPaymentUseCase;
import br.com.spbank.pix.application.port.in.PixPreview;
import br.com.spbank.pix.application.port.out.PixPersistence;
import br.com.spbank.pix.application.service.PixKeyNormalizer;
import br.com.spbank.pix.application.service.PixProperties;
import br.com.spbank.pix.application.service.PixRequestFingerprint;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PixService
        implements PixKeyUseCase,
        PixPaymentUseCase,
        PixFavoriteUseCase {

    private final AccountPersistence accounts;
    private final PixPersistence pix;
    private final PixExecutionService execution;
    private final BankInstitutionCatalog banks;
    private final PixProperties properties;
    private final Clock clock;

    public PixService(
        AccountPersistence accounts,
        PixPersistence pix,
        PixExecutionService execution,
        BankInstitutionCatalog banks,
        PixProperties properties,
        Clock clock
    ) {
        this.accounts = accounts;
        this.pix = pix;
        this.execution = execution;
        this.banks = banks;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PixKey registerKey(
        UUID accountId,
        PixKeyType type,
        String value
    ) {
        Account account =
            active(
                findAccount(accountId)
            );

        long limit =
            account
                .getHolderDocument()
                .length() == 14
                ? 20
                : 5;

        if (pix.countActiveKeys(accountId)
            >= limit) {

            throw new BusinessException(
                "PIX_KEY_LIMIT_EXCEEDED",
                "pix.key.limit-exceeded"
            );
        }

        String candidate =
            switch (type) {

                case CPF_CNPJ ->
                    account.getHolderDocument();

                case RANDOM ->
                    value == null
                        || value.isBlank()
                            ? UUID
                                .randomUUID()
                                .toString()
                            : value;

                default ->
                    value;
            };

        String normalized =
            PixKeyNormalizer.normalize(
                type,
                candidate
            );

        if (pix.findActiveKey(
                type,
                normalized
            ).isPresent()) {

            throw new BusinessException(
                "PIX_KEY_ALREADY_REGISTERED",
                "pix.key.already-registered"
            );
        }

        return pix.saveKey(
            PixKey.create(
                accountId,
                type,
                normalized,
                clock.instant()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PixKey> listKeys(
        UUID accountId
    ) {
        return pix.findActiveKeys(
            accountId
        );
    }

    @Override
    @Transactional
    public void deactivateKey(
        UUID accountId,
        UUID keyId
    ) {
        PixKey key =
            pix.findKey(
                keyId,
                accountId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        "PIX_KEY_ACCESS_DENIED",
                        "pix.key.access-denied"
                    )
            );

        key.deactivate();

        pix.saveKey(key);

        pix.deleteFavoritesByPixKeyId(
            key.getId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PixPreview preview(
        PixCreationCommand command
    ) {
        Prepared prepared =
            prepare(
                command,
                false
            );

        return new PixPreview(
            prepared
                .destination()
                .recipientName(),

            prepared
                .destination()
                .maskedRecipientDocument(),

            prepared
                .destination()
                .bankCode(),

            prepared
                .destination()
                .bankName(),

            command.keyType(),

            PixKeyNormalizer.mask(
                command.keyType(),
                prepared.normalizedKey()
            ),

            prepared.amount(),

            effectiveScheduledFor(
                command
            ),

            command.frequency(),
            command.occurrences()
        );
    }

    @Override
    @Transactional
    public PixPayment create(
        PixCreationCommand command
    ) {
        if (command.idempotencyKey()
            == null) {

            throw new BusinessException(
                "INVALID_IDEMPOTENCY_KEY",
                "request.invalid"
            );
        }

        String normalized =
            PixKeyNormalizer.normalize(
                command.keyType(),
                command.key()
            );

        String fingerprint =
            PixRequestFingerprint.calculate(
                command,
                normalized
            );

        Optional<PixPayment> previous =
            pix.findBySourceAndIdempotencyKey(
                command.sourceAccountId(),
                command.idempotencyKey()
            );

        if (previous.isPresent()) {
            return sameRequest(
                previous.get(),
                fingerprint
            );
        }

        Prepared prepared =
            prepare(
                command,
                true
            );

        List<UUID> accountIds =
            prepared.destination().routing()
                == PixRouting.INTERNAL

                ? List.of(
                    prepared.source().getId(),
                    prepared.destination().accountId()
                )

                : List.of(
                    prepared.source().getId()
                );

        Map<UUID, Account> locked =
            accounts.findAllForUpdate(
                accountIds
            );

        Account source =
            active(
                locked.get(
                    prepared.source().getId()
                )
            );

        source.requireAvailable(
            prepared.amount()
        );

        previous =
            pix.findBySourceAndIdempotencyKey(
                command.sourceAccountId(),
                command.idempotencyKey()
            );

        if (previous.isPresent()) {
            return sameRequest(
                previous.get(),
                fingerprint
            );
        }

        String maskedKey =
            PixKeyNormalizer.mask(
                command.keyType(),
                normalized
            );

        String keyHash =
            PixKeyNormalizer.sha256(
                normalized
            );

        var now =
            clock.instant();

        PixPayment result;

        if (command.frequency()
            != null) {

            UUID recurrenceId =
                UUID.randomUUID();

            List<PixPayment> series =
                new ArrayList<>();

            for (
                int index = 0;
                index
                    < command.occurrences();
                index++
            ) {
                LocalDate date =
                    recurrenceDate(
                        command.scheduledFor(),
                        command.frequency(),
                        index
                    );

                UUID key =
                    index == 0
                        ? command.idempotencyKey()
                        : UUID.nameUUIDFromBytes(
                            (
                                command.idempotencyKey()
                                    + ":"
                                    + (index + 1)
                            )
                            .getBytes(
                                StandardCharsets.UTF_8
                            )
                        );

                series.add(
                    PixPayment.scheduled(
                        source.getId(),
                        prepared.destination(),
                        command.keyType(),
                        maskedKey,
                        keyHash,
                        prepared.amount(),
                        key,
                        fingerprint,
                        now,
                        date,
                        recurrenceId,
                        command.frequency(),
                        index + 1,
                        command.occurrences()
                    )
                );
            }

            result =
                pix.savePayments(
                    series
                )
                .stream()
                .filter(
                    item ->
                        Integer.valueOf(1)
                            .equals(
                                item.getOccurrenceNumber()
                            )
                )
                .findFirst()
                .orElseThrow();

        } else if (
            command.scheduledFor()
                != null
            && command
                .scheduledFor()
                .isAfter(today())
        ) {

            result =
                pix.savePayment(
                    PixPayment.scheduled(
                        source.getId(),
                        prepared.destination(),
                        command.keyType(),
                        maskedKey,
                        keyHash,
                        prepared.amount(),
                        command.idempotencyKey(),
                        fingerprint,
                        now,
                        command.scheduledFor(),
                        null,
                        null,
                        null,
                        null
                    )
                );

        } else {

            PixPayment processing =
                pix.savePayment(
                    PixPayment.processing(
                        source.getId(),
                        prepared.destination(),
                        command.keyType(),
                        maskedKey,
                        keyHash,
                        prepared.amount(),
                        command.idempotencyKey(),
                        fingerprint,
                        now
                    )
                );

            result =
                execution.executeLocked(
                    processing
                );
        }

        if (command.saveFavorite()
            && prepared.key() != null
            && prepared.favorite().isEmpty()) {

            pix.saveFavorite(
                PixFavorite.internal(
                    source.getId(),
                    prepared.key().getId(),
                    prepared
                        .destination()
                        .recipientName(),
                    prepared.normalizedKey(),
                    maskedKey,
                    command.keyType(),
                    prepared
                        .destination()
                        .maskedRecipientDocument(),
                    now
                )
            );
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PixPayment get(
        UUID accountId,
        UUID id
    ) {
        PixPayment payment =
            pix.findPayment(id)
                .orElseThrow(
                    () ->
                        new NotFoundException(
                            "PIX_NOT_FOUND"
                        )
                );

        requireOwner(
            payment,
            accountId
        );

        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PixPayment> listScheduled(
        UUID accountId
    ) {
        return pix.findScheduled(
            accountId,
            100
        );
    }

    @Override
    @Transactional
    public PixPayment cancel(
        UUID accountId,
        UUID id
    ) {
        PixPayment payment =
            pix.findPaymentForUpdate(id)
                .orElseThrow(
                    () ->
                        new NotFoundException(
                            "PIX_NOT_FOUND"
                        )
                );

        requireOwner(
            payment,
            accountId
        );

        LocalDate today =
            today();

        payment.cancel(
            clock.instant(),
            today
        );

        return pix.savePayment(
            payment
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PixFavorite> listFavorites(
        UUID accountId
    ) {
        return pix.findFavorites(
            accountId
        );
    }

    @Override
    @Transactional
    public PixFavorite registerFavorite(
        PixFavoriteRegistrationCommand command
    ) {
        Account source =
            active(
                findAccount(
                    command.sourceAccountId()
                )
            );

        String normalized =
            PixKeyNormalizer.normalize(
                command.keyType(),
                command.key()
            );

        if (
            pix.findFavorite(
                command.sourceAccountId(),
                command.keyType(),
                normalized
            ).isPresent()
        ) {
            throw new BusinessException(
                "PIX_FAVORITE_ALREADY_EXISTS",
                "pix.favorite.already-exists"
            );
        }

        Optional<PixKey> localKey =
            pix.findActiveKey(
                command.keyType(),
                normalized
            );

        String bankCode =
            Objects.requireNonNullElse(
                command.bankCode(),
                ""
            ).trim();

        BankInstitution bank =
            banks.findActiveByCode(
                bankCode
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        "PIX_FAVORITE_BANK_INVALID",
                        "pix.favorite.bank-invalid"
                    )
            );

        if (localKey.isPresent()) {
            Account target =
                active(
                    findAccount(
                        localKey
                            .get()
                            .getAccountId()
                    )
                );

            if (
                source.getId().equals(
                    target.getId()
                )
            ) {
                throw new BusinessException(
                    "PIX_SAME_ACCOUNT",
                    "pix.same-account"
                );
            }

            if (
                !bank.internal()
                || !bank.studyCode().equals(
                    target.getBankCode()
                )
            ) {
                throw new BusinessException(
                    "PIX_FAVORITE_BANK_INVALID",
                    "pix.favorite.bank-invalid"
                );
            }

            return pix.saveFavorite(
                PixFavorite.internal(
                    source.getId(),
                    localKey.get().getId(),
                    target.getHolderName(),
                    normalized,
                    PixKeyNormalizer.mask(
                        command.keyType(),
                        normalized
                    ),
                    command.keyType(),
                    PixKeyNormalizer.maskDocument(
                        target.getHolderDocument()
                    ),
                    clock.instant()
                )
            );
        }

        if (bank.internal()) {
            throw new BusinessException(
                "PIX_FAVORITE_BANK_INVALID",
                "pix.favorite.bank-invalid"
            );
        }

        String name =
            Objects.requireNonNullElse(
                command.recipientName(),
                ""
            ).trim();

        if (name.isBlank()) {
            throw new BusinessException(
                "PIX_FAVORITE_NAME_REQUIRED",
                "pix.favorite.name.required"
            );
        }

        String maskedDocument =
            maskOptionalDocument(
                command.recipientDocument()
            );

        return pix.saveFavorite(
            PixFavorite.external(
                command.sourceAccountId(),
                name,
                normalized,
                PixKeyNormalizer.mask(
                    command.keyType(),
                    normalized
                ),
                command.keyType(),
                bank.studyCode(),
                bank.name(),
                maskedDocument,
                clock.instant()
            )
        );
    }

    @Override
    @Transactional
    public void deleteFavorite(
        UUID accountId,
        UUID favoriteId
    ) {
        PixFavorite favorite =
            pix.findFavoriteById(
                favoriteId,
                accountId
            )
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "PIX_FAVORITE_NOT_FOUND"
                    )
            );

        pix.deleteFavorite(
            favorite
        );
    }

    @Override
    @Transactional
    public int cancelRecurrence(
        UUID accountId,
        UUID recurrenceId
    ) {
        List<PixPayment> pending =
            pix.findScheduledByRecurrence(
                accountId,
                recurrenceId
            );

        if (pending.isEmpty()) {
            throw new NotFoundException(
                "PIX_RECURRENCE_NOT_FOUND"
            );
        }

        LocalDate today =
            today();

        int cancelled = 0;

        for (PixPayment payment : pending) {
            if (
                payment
                    .getScheduledFor()
                    .isAfter(today)
            ) {
                payment.cancel(
                    clock.instant(),
                    today
                );

                pix.savePayment(
                    payment
                );

                cancelled++;
            }
        }

        if (cancelled == 0) {
            throw new BusinessException(
                "PIX_CANNOT_BE_CANCELLED",
                "pix.cannot-cancel"
            );
        }

        return cancelled;
    }

    private Prepared prepare(
        PixCreationCommand command,
        boolean validateSchedule
    ) {
        BigDecimal amount =
            money(
                command.amount()
            );

        if (amount.signum() <= 0) {
            throw new BusinessException(
                "INVALID_PIX_AMOUNT",
                "account.invalid-amount"
            );
        }

        if (
            amount.compareTo(
                properties.maxPerOperation()
            ) > 0
        ) {
            throw new BusinessException(
                "PIX_LIMIT_EXCEEDED",
                "pix.limit-exceeded"
            );
        }

        String normalized =
            PixKeyNormalizer.normalize(
                command.keyType(),
                command.key()
            );

        Account source =
            active(
                findAccount(
                    command.sourceAccountId()
                )
            );

        source.requireAvailable(
            amount
        );

        Optional<PixKey> key =
            pix.findActiveKey(
                command.keyType(),
                normalized
            );

        Optional<PixFavorite> favorite =
            pix.findUsableFavorite(
                source.getId(),
                command.keyType(),
                normalized
            );

        PixDestination destination;

        if (key.isPresent()) {
            Account target =
                active(
                    findAccount(
                        key
                            .get()
                            .getAccountId()
                    )
                );

            if (
                source.getId().equals(
                    target.getId()
                )
            ) {
                throw new BusinessException(
                    "PIX_SAME_ACCOUNT",
                    "pix.same-account"
                );
            }

            destination =
                PixDestination.internal(
                    target.getId(),
                    target.getHolderName(),
                    PixKeyNormalizer.maskDocument(
                        target.getHolderDocument()
                    ),
                    target.getBankCode()
                );

        } else {

            if (
                command.saveFavorite()
                && favorite.isEmpty()
            ) {
                throw new BusinessException(
                    "PIX_EXTERNAL_FAVORITE_REGISTRATION_REQUIRED",
                    "pix.external.favorite-registration-required"
                );
            }

            destination =
                favorite
                    .filter(
                        item ->
                            item.routing()
                                == PixRouting.EXTERNAL
                    )
                    .map(
                        item ->
                            PixDestination.external(
                                item.recipientName(),
                                item.maskedRecipientDocument(),
                                item.bankCode(),
                                item.bankName()
                            )
                    )
                    .orElseGet(
                        () ->
                            PixDestination.external(
                                "Conta de outra instituição",
                                command.keyType()
                                    == PixKeyType.CPF_CNPJ

                                    ? PixKeyNormalizer
                                        .maskDocument(
                                            normalized
                                        )

                                    : "Não informado",
                                "EXTERNAL",
                                "Outra instituição"
                            )
                    );
        }

        if (
            amount.compareTo(
                properties.favoriteRequiredAbove()
            ) > 0
            && favorite.isEmpty()
        ) {
            throw new BusinessException(
                "PIX_FAVORITE_REQUIRED",
                "pix.favorite.required"
            );
        }

        if (
            validateSchedule
            || command.scheduledFor() != null
            || command.frequency() != null
        ) {
            validateRecurrence(
                command
            );
        }

        return new Prepared(
            source,
            key.orElse(null),
            favorite,
            destination,
            normalized,
            amount
        );
    }

    private void validateRecurrence(
        PixCreationCommand command
    ) {
        LocalDate today =
            today();

        if (
            command.scheduledFor() != null
            && command
                .scheduledFor()
                .isBefore(today)
        ) {
            throw new BusinessException(
                "INVALID_PIX_DATE",
                "pix.date.invalid"
            );
        }

        boolean hasFrequency =
            command.frequency() != null;

        boolean hasOccurrences =
            command.occurrences() != null;

        if (hasFrequency
            != hasOccurrences) {

            throw new BusinessException(
                "INVALID_PIX_RECURRENCE",
                "pix.recurrence.invalid"
            );
        }

        if (hasFrequency) {
            if (
                command.scheduledFor() == null
                || !command
                    .scheduledFor()
                    .isAfter(today)
            ) {
                throw new BusinessException(
                    "PIX_RECURRENCE_DATE_REQUIRED",
                    "pix.recurrence.date-required"
                );
            }

            if (
                command.occurrences() < 2
                || command.occurrences() > 24
            ) {
                throw new BusinessException(
                    "INVALID_PIX_RECURRENCE",
                    "pix.recurrence.invalid"
                );
            }
        }
    }

    private Account findAccount(
        UUID id
    ) {
        return accounts.findById(id)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "ACCOUNT_NOT_FOUND"
                    )
            );
    }

    private static PixPayment sameRequest(
        PixPayment previous,
        String fingerprint
    ) {
        if (
            !fingerprint.equals(
                previous.getRequestFingerprint()
            )
        ) {
            throw new BusinessException(
                "PIX_IDEMPOTENCY_KEY_REUSED",
                "pix.idempotency-key-reused"
            );
        }

        return previous;
    }

    private static void requireOwner(
        PixPayment payment,
        UUID accountId
    ) {
        if (
            !payment
                .getSourceAccountId()
                .equals(accountId)
        ) {
            throw new BusinessException(
                "PIX_ACCESS_DENIED",
                "pix.access-denied"
            );
        }
    }

    private static Account active(
        Account account
    ) {
        if (!account.isActive()) {
            throw new AccountInactiveException();
        }

        return account;
    }

    private static BigDecimal money(
        BigDecimal value
    ) {
        if (value == null) {
            throw new BusinessException(
                "INVALID_PIX_AMOUNT",
                "account.invalid-amount"
            );
        }

        try {
            return value.setScale(
                2,
                RoundingMode.UNNECESSARY
            );

        } catch (ArithmeticException ex) {
            throw new BusinessException(
                "INVALID_PIX_AMOUNT",
                "account.invalid-amount"
            );
        }
    }

    private static String maskOptionalDocument(
        String document
    ) {
        if (
            document == null
            || document.isBlank()
        ) {
            return "Não informado";
        }

        String normalized =
            PixKeyNormalizer.normalize(
                PixKeyType.CPF_CNPJ,
                document
            );

        return PixKeyNormalizer.maskDocument(
            normalized
        );
    }

    static LocalDate recurrenceDate(
        LocalDate first,
        PixFrequency frequency,
        int index
    ) {
        return frequency
            == PixFrequency.WEEKLY

            ? first.plusWeeks(index)

            : first.plusMonths(index);
    }

    private LocalDate today() {
        return clock
            .instant()
            .atZone(
                ZoneId.of(
                    properties.zone()
                )
            )
            .toLocalDate();
    }

    private LocalDate effectiveScheduledFor(
        PixCreationCommand command
    ) {
        if (
            command.scheduledFor()
                == null
        ) {
            return null;
        }

        return command.frequency() != null
            || command
                .scheduledFor()
                .isAfter(today())

            ? command.scheduledFor()

            : null;
    }

    private record Prepared(
        Account source,
        PixKey key,
        Optional<PixFavorite> favorite,
        PixDestination destination,
        String normalizedKey,
        BigDecimal amount
    ) {
    }
}