package br.com.spbank.pix.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class PixPayment {

    private final UUID id;
    private final UUID originAccountId;
    private final UUID destinationAccountId;
    private final PixDestinationScope destinationScope;
    private final PixKeyType keyType;
    private final String maskedKey;
    private final String keyHash;
    private final String holderName;
    private final String maskedDocument;
    private final String bankCode;
    private final String bankName;
    private final BigDecimal amount;
    private PixStatus status;
    private final UUID idempotencyKey;
    private final String requestHash;
    private final Instant requestedAt;
    private final LocalDate scheduledFor;
    private Instant processedAt;
    private String failureCode;
    private String failureMessage;
    private final UUID recurrenceId;
    private final PixRecurrenceFrequency recurrenceFrequency;
    private final Integer occurrenceNumber;
    private final Integer totalOccurrences;
    private String settlementReference;

    public PixPayment(
        UUID id,
        UUID originAccountId,
        UUID destinationAccountId,
        PixDestinationScope destinationScope,
        PixKeyType keyType,
        String maskedKey,
        String keyHash,
        String holderName,
        String maskedDocument,
        String bankCode,
        String bankName,
        BigDecimal amount,
        PixStatus status,
        UUID idempotencyKey,
        String requestHash,
        Instant requestedAt,
        LocalDate scheduledFor,
        Instant processedAt,
        String failureCode,
        String failureMessage,
        UUID recurrenceId,
        PixRecurrenceFrequency recurrenceFrequency,
        Integer occurrenceNumber,
        Integer totalOccurrences,
        String settlementReference
    ) {
        this.id = Objects.requireNonNull(id);
        this.originAccountId = Objects.requireNonNull(originAccountId);
        this.destinationScope = Objects.requireNonNull(destinationScope);
        this.keyType = Objects.requireNonNull(keyType);
        this.maskedKey = Objects.requireNonNull(maskedKey);
        this.keyHash = Objects.requireNonNull(keyHash);
        this.holderName = Objects.requireNonNull(holderName);
        this.maskedDocument = Objects.requireNonNull(maskedDocument);
        this.bankCode = Objects.requireNonNull(bankCode);
        this.bankName = Objects.requireNonNull(bankName);
        this.amount = Objects.requireNonNull(amount);
        this.status = Objects.requireNonNull(status);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.requestHash = Objects.requireNonNull(requestHash);
        this.requestedAt = Objects.requireNonNull(requestedAt);

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                "Valor do PIX deve ser maior que zero"
            );
        }

        if (destinationScope == PixDestinationScope.INTERNAL
            && destinationAccountId == null) {
            throw new IllegalArgumentException(
                "Conta de destino é obrigatória para PIX interno"
            );
        }

        if (destinationScope == PixDestinationScope.EXTERNAL
            && destinationAccountId != null) {
            throw new IllegalArgumentException(
                "Conta de destino não deve ser informada para PIX externo"
            );
        }

        if ((recurrenceId == null)
            != (recurrenceFrequency == null)) {
            throw new IllegalArgumentException(
                "Dados de recorrência devem ser informados em conjunto"
            );
        }

        if (recurrenceId == null) {
            if (occurrenceNumber != null || totalOccurrences != null) {
                throw new IllegalArgumentException(
                    "Ocorrência não deve ser informada sem recorrência"
                );
            }
        } else {
            if (occurrenceNumber == null || totalOccurrences == null) {
                throw new IllegalArgumentException(
                    "Ocorrência e total são obrigatórios para recorrência"
                );
            }

            if (occurrenceNumber < 1
                || totalOccurrences < 2
                || totalOccurrences > 24
                || occurrenceNumber > totalOccurrences) {
                throw new IllegalArgumentException(
                    "Dados de recorrência inválidos"
                );
            }
        }

        this.destinationAccountId = destinationAccountId;
        this.scheduledFor = scheduledFor;
        this.processedAt = processedAt;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.recurrenceId = recurrenceId;
        this.recurrenceFrequency = recurrenceFrequency;
        this.occurrenceNumber = occurrenceNumber;
        this.totalOccurrences = totalOccurrences;
        this.settlementReference = settlementReference;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOriginAccountId() {
        return originAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public PixDestinationScope getDestinationScope() {
        return destinationScope;
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

    public String getHolderName() {
        return holderName;
    }

    public String getMaskedDocument() {
        return maskedDocument;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBankName() {
        return bankName;
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

    public String getRequestHash() {
        return requestHash;
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

    public String getFailureMessage() {
        return failureMessage;
    }

    public UUID getRecurrenceId() {
        return recurrenceId;
    }

    public PixRecurrenceFrequency getRecurrenceFrequency() {
        return recurrenceFrequency;
    }

    public Integer getOccurrenceNumber() {
        return occurrenceNumber;
    }

    public Integer getTotalOccurrences() {
        return totalOccurrences;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public boolean isScheduled() {
        return status == PixStatus.SCHEDULED;
    }

    public boolean isProcessing() {
        return status == PixStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return status == PixStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PixStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == PixStatus.CANCELLED;
    }

    public boolean isRecurring() {
        return recurrenceId != null;
    }

    public void markProcessing() {
        ensureStatus(PixStatus.SCHEDULED);

        this.status = PixStatus.PROCESSING;
    }

    public void markCompleted(
        Instant processedAt,
        String settlementReference
    ) {
        Objects.requireNonNull(processedAt);

        if (status != PixStatus.PROCESSING
            && status != PixStatus.SCHEDULED) {
            throw new IllegalStateException(
                "PIX não pode ser concluído a partir do estado atual"
            );
        }

        this.status = PixStatus.COMPLETED;
        this.processedAt = processedAt;
        this.settlementReference = settlementReference;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markFailed(
        Instant processedAt,
        String failureCode,
        String failureMessage
    ) {
        Objects.requireNonNull(processedAt);
        Objects.requireNonNull(failureCode);
        Objects.requireNonNull(failureMessage);

        if (status != PixStatus.PROCESSING
            && status != PixStatus.SCHEDULED) {
            throw new IllegalStateException(
                "PIX não pode entrar em falha a partir do estado atual"
            );
        }

        this.status = PixStatus.FAILED;
        this.processedAt = processedAt;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public void cancel() {
        if (status != PixStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Somente PIX agendado pode ser cancelado"
            );
        }

        this.status = PixStatus.CANCELLED;
    }

    private void ensureStatus(PixStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException(
                "Estado atual do PIX não permite esta operação"
            );
        }
    }

    @Override
    public String toString() {
        return "PixPayment[" +
            "id=" + id +
            ", originAccountId=" + originAccountId +
            ", destinationAccountId=" + destinationAccountId +
            ", destinationScope=" + destinationScope +
            ", keyType=" + keyType +
            ", maskedKey=" + maskedKey +
            ", keyHash=[REDACTED]" +
            ", holderName=" + holderName +
            ", maskedDocument=" + maskedDocument +
            ", bankCode=" + bankCode +
            ", bankName=" + bankName +
            ", amount=" + amount +
            ", status=" + status +
            ", idempotencyKey=" + idempotencyKey +
            ", requestHash=[REDACTED]" +
            ", requestedAt=" + requestedAt +
            ", scheduledFor=" + scheduledFor +
            ", processedAt=" + processedAt +
            ", failureCode=" + failureCode +
            ", failureMessage=" + failureMessage +
            ", recurrenceId=" + recurrenceId +
            ", recurrenceFrequency=" + recurrenceFrequency +
            ", occurrenceNumber=" + occurrenceNumber +
            ", totalOccurrences=" + totalOccurrences +
            ", settlementReference=" + settlementReference +
            ']';
    }
}