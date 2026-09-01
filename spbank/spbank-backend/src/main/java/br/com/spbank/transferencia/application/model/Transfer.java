package br.com.spbank.transferencia.application.model;

import br.com.spbank.shared.application.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class Transfer {

    private final UUID id;
    private final TransferType type;
    private final UUID sourceAccountId;
    private final UUID targetAccountId;
    private final TransferRecipient recipient;
    private final BigDecimal amount;

    private BigDecimal fee;
    private boolean feeCalculated;

    private final UUID idempotencyKey;
    private final String requestFingerprint;

    private TransferStatus status;

    private final Instant requestedAt;
    private final LocalDate scheduledFor;

    private Instant processedAt;

    private String failureCode;
    private String failureMessageKey;

    private String settlementReference;

    public Transfer(
            UUID id,
            TransferType type,
            UUID sourceAccountId,
            UUID targetAccountId,
            TransferRecipient recipient,
            BigDecimal amount,
            BigDecimal fee,
            boolean feeCalculated,
            UUID idempotencyKey,
            String requestFingerprint,
            TransferStatus status,
            Instant requestedAt,
            LocalDate scheduledFor,
            Instant processedAt,
            String failureCode,
            String failureMessageKey,
            String settlementReference) {

        this.id = id;
        this.type = type;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.recipient = recipient;
        this.amount = amount;
        this.fee = fee;
        this.feeCalculated = feeCalculated;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = status;
        this.requestedAt = requestedAt;
        this.scheduledFor = scheduledFor;
        this.processedAt = processedAt;
        this.failureCode = failureCode;
        this.failureMessageKey = failureMessageKey;
        this.settlementReference = settlementReference;
    }

    public static Transfer processing(
            TransferType type,
            UUID sourceId,
            UUID targetId,
            TransferRecipient recipient,
            BigDecimal amount,
            BigDecimal fee,
            boolean feeCalculated,
            UUID key,
            String requestFingerprint,
            Instant requestedAt) {

        return new Transfer(
                UUID.randomUUID(),
                type,
                sourceId,
                targetId,
                recipient,
                amount,
                fee,
                feeCalculated,
                key,
                requestFingerprint,
                TransferStatus.PROCESSING,
                requestedAt,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static Transfer scheduled(
            TransferType type,
            UUID sourceId,
            UUID targetId,
            TransferRecipient recipient,
            BigDecimal amount,
            BigDecimal fee,
            boolean feeCalculated,
            UUID key,
            String requestFingerprint,
            LocalDate date,
            Instant requestedAt) {

        return new Transfer(
                UUID.randomUUID(),
                type,
                sourceId,
                targetId,
                recipient,
                amount,
                fee,
                feeCalculated,
                key,
                requestFingerprint,
                TransferStatus.SCHEDULED,
                requestedAt,
                date,
                null,
                null,
                null,
                null
        );
    }

    public void requireDue(LocalDate today) {

        if (status != TransferStatus.SCHEDULED) {
            throw new BusinessException(
                    "INVALID_TRANSFER_STATUS",
                    "transfer.not-scheduled"
            );
        }

        if (scheduledFor == null || scheduledFor.isAfter(today)) {
            throw new BusinessException(
                    "TRANSFER_NOT_DUE",
                    "transfer.not-due"
            );
        }
    }

    public void startProcessing() {
        status = TransferStatus.PROCESSING;
    }

    public void defineFee(BigDecimal value) {
        fee = value;
        feeCalculated = true;
    }

    public void registerSettlement(String reference) {
        settlementReference = reference;
    }

    public void complete(Instant at) {
        status = TransferStatus.COMPLETED;
        processedAt = at;
    }

    public void fail(
            String code,
            String key,
            Instant at) {

        status = TransferStatus.FAILED;
        failureCode = code;
        failureMessageKey = key;
        processedAt = at;
    }

    public void cancel(Instant at) {

        if (status != TransferStatus.SCHEDULED) {
            throw new BusinessException(
                    "TRANSFER_CANNOT_BE_CANCELLED",
                    "transfer.cannot-cancel"
            );
        }

        status = TransferStatus.CANCELLED;
        processedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public TransferType getType() {
        return type;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public TransferRecipient getRecipient() {
        return recipient;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public boolean isFeeCalculated() {
        return feeCalculated;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public TransferStatus getStatus() {
        return status;
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

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessageKey() {
        return failureMessageKey;
    }

    public String getSettlementReference() {
        return settlementReference;
    }
}