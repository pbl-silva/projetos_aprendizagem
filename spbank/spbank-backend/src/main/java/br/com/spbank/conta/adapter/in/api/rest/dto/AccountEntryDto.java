package br.com.spbank.conta.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.EntryDirection;
import br.com.spbank.conta.application.model.EntryReferenceType;
import br.com.spbank.conta.application.model.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountEntryDto(
        UUID id,
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
        String operationType
) {
}