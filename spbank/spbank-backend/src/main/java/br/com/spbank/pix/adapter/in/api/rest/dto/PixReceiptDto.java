package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixFrequency;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PixReceiptDto(
    UUID id,
    PixStatus status,
    BigDecimal amount,
    String recipientName,
    String maskedRecipientDocument,
    PixKeyType keyType,
    String maskedKey,
    String destinationBankCode,
    String destinationBankName,
    String settlementReference,
    LocalDate scheduledFor,
    Instant requestedAt,
    Instant processedAt,
    String failureCode,
    UUID recurrenceId,
    PixFrequency frequency,
    Integer occurrenceNumber,
    Integer totalOccurrences
) {
}