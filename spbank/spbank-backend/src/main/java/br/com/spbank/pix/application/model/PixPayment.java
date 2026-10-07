package br.com.spbank.pix.application.model;

import br.com.spbank.shared.application.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PixPayment {

    private final UUID id;
    private final UUID sourceAccountId;
    private final UUID targetAccountId;
    private final PixRouting routing;
    private final PixKeyType keyType;
    private final String maskedKey;
    private final String keyHash;
    private final String recipientName;
    private final String maskedRecipientDocument;
    private final String destinationBankCode;
    private final String destinationBankName;
    private final BigDecimal amount;

    private PixStatus status;

    private final UUID idempotencyKey;
    private final String requestFingerprint;
    private final Instant requestedAt;
    private final LocalDate scheduledFor;

    private Instant processedAt;
    private String settlementReference;
    private String failureCode;
    private String failureMessageKey;

    private final UUID recurrenceId;
    private final PixFrequency frequency;
    private final Integer occurrenceNumber;
    private final Integer totalOccurrences;

    public PixPayment(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        PixRouting routing,
        PixKeyType keyType,
        String maskedKey,
        String keyHash,
        String recipientName,
        String maskedRecipientDocument,
        String destinationBankCode,
        String destinationBankName,
        BigDecimal amount,
        PixStatus status,
        UUID idempotencyKey,
        String requestFingerprint,
        Instant requestedAt,
        LocalDate scheduledFor,
        Instant processedAt,
        String settlementReference,
        String failureCode,
        String failureMessageKey,
        UUID recurrenceId,
        PixFrequency frequency,
        Integer occurrenceNumber,
        Integer totalOccurrences
    ) {
        this.id = id;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.routing = routing;
        this.keyType = keyType;
        this.maskedKey = maskedKey;
        this.keyHash = keyHash;
        this.recipientName = recipientName;
        this.maskedRecipientDocument =
            maskedRecipientDocument;
        this.destinationBankCode =
            destinationBankCode;
        this.destinationBankName =
            destinationBankName;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint =
            requestFingerprint;
        this.requestedAt = requestedAt;
        this.scheduledFor = scheduledFor;
        this.processedAt = processedAt;
        this.settlementReference =
            settlementReference;
        this.failureCode = failureCode;
        this.failureMessageKey =
            failureMessageKey;
        this.recurrenceId = recurrenceId;
        this.frequency = frequency;
        this.occurrenceNumber =
            occurrenceNumber;
        this.totalOccurrences =
            totalOccurrences;
    }

    public static PixPayment processing(
        UUID sourceId,
        PixDestination destination,
        PixKeyType keyType,
        String maskedKey,
        String keyHash,
        BigDecimal amount,
        UUID idempotencyKey,
        String fingerprint,
        Instant now
    ) {
        return new PixPayment(
            UUID.randomUUID(),
            sourceId,
            destination.accountId(),
            destination.routing(),
            keyType,
            maskedKey,
            keyHash,
            destination.recipientName(),
            destination.maskedRecipientDocument(),
            destination.bankCode(),
            destination.bankName(),
            amount,
            PixStatus.PROCESSING,
            idempotencyKey,
            fingerprint,
            now,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static PixPayment scheduled(
        UUID sourceId,
        PixDestination destination,
        PixKeyType keyType,
        String maskedKey,
        String keyHash,
        BigDecimal amount,
        UUID idempotencyKey,
        String fingerprint,
        Instant now,
        LocalDate date,
        UUID recurrenceId,
        PixFrequency frequency,
        Integer occurrence,
        Integer total
    ) {
        return new PixPayment(
            UUID.randomUUID(),
            sourceId,
            destination.accountId(),
            destination.routing(),
            keyType,
            maskedKey,
            keyHash,
            destination.recipientName(),
            destination.maskedRecipientDocument(),
            destination.bankCode(),
            destination.bankName(),
            amount,
            PixStatus.SCHEDULED,
            idempotencyKey,
            fingerprint,
            now,
            date,
            null,
            null,
            null,
            null,
            recurrenceId,
            frequency,
            occurrence,
            total
        );
    }

    public void requireDue(
        LocalDate today
    ) {
        if (status != PixStatus.SCHEDULED) {
            throw new BusinessException(
                "PIX_NOT_SCHEDULED",
                "pix.not-scheduled"
            );
        }

        if (scheduledFor == null
            || scheduledFor.isAfter(today)) {

            throw new BusinessException(
                "PIX_NOT_DUE",
                "pix.not-due"
            );
        }
    }

    public void startProcessing() {
        status = PixStatus.PROCESSING;
    }

    public void complete(
        Instant at,
        String reference
    ) {
        status = PixStatus.COMPLETED;
        processedAt = at;
        settlementReference = reference;
    }

    public void fail(
        String code,
        String key,
        Instant at
    ) {
        status = PixStatus.FAILED;
        failureCode = code;
        failureMessageKey = key;
        processedAt = at;
    }

    public void cancel(
        Instant at,
        LocalDate today
    ) {
        if (status != PixStatus.SCHEDULED
            || scheduledFor == null
            || !scheduledFor.isAfter(today)) {

            throw new BusinessException(
                "PIX_CANNOT_BE_CANCELLED",
                "pix.cannot-cancel"
            );
        }

        status = PixStatus.CANCELLED;
        processedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public PixRouting getRouting() {
        return routing;
    }

    public PixKeyType getKeyType() {
        return keyType;
    }

    public String getMaskedKey() {
        return maskedKey;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getMaskedRecipientDocument() {
        return maskedRecipientDocument;
    }

    public String getDestinationBankCode() {
        return destinationBankCode;
    }

    public String getDestinationBankName() {
        return destinationBankName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PixStatus getStatus() {
        return status;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public LocalDate getScheduledFor() {
        return scheduledFor;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessageKey() {
        return failureMessageKey;
    }

    public UUID getRecurrenceId() {
        return recurrenceId;
    }

    public PixFrequency getFrequency() {
        return frequency;
    }

    public Integer getOccurrenceNumber() {
        return occurrenceNumber;
    }

    public Integer getTotalOccurrences() {
        return totalOccurrences;
    }
}