package br.com.spbank.conta.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountEntry(
        UUID id,
        UUID accountId,
        UUID referenceId,
        EntryReferenceType referenceType,
        EntryType type,
        EntryDirection direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant occurredAt,
        String counterpartyName,
        String counterpartyBankCode,
        String operationType) {

    public static AccountEntry create(
            Account account,
            UUID referenceId,
            EntryReferenceType referenceType,
            EntryType type,
            EntryDirection direction,
            BigDecimal amount,
            String description,
            Instant at) {

        return new AccountEntry(
                UUID.randomUUID(),
                account.getId(),
                referenceId,
                referenceType,
                type,
                direction,
                amount,
                account.getBalance(),
                description,
                at,
                null,
                null,
                null
        );
    }

    public static AccountEntry create(
            Account account,
            UUID referenceId,
            EntryReferenceType referenceType,
            EntryType type,
            EntryDirection direction,
            BigDecimal amount,
            String description,
            Instant at,
            String counterpartyName,
            String counterpartyBankCode,
            String operationType) {

        return new AccountEntry(
                UUID.randomUUID(),
                account.getId(),
                referenceId,
                referenceType,
                type,
                direction,
                amount,
                account.getBalance(),
                description,
                at,
                counterpartyName,
                counterpartyBankCode,
                operationType
        );
    }
}