package br.com.spbank.pix.adapter.out.persistence.mysql.data;

import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixFrequencyConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixKeyTypeConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixRoutingConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixStatusConverter;
import br.com.spbank.pix.application.model.PixFrequency;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixRouting;
import br.com.spbank.pix.application.model.PixStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pix")
public class PixPaymentData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "conta_origem_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID sourceAccountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "conta_destino_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID targetAccountId;

    @Convert(converter = PixRoutingConverter.class)
    @Column(name = "escopo_destino")
    private PixRouting routing;

    @Convert(converter = PixKeyTypeConverter.class)
    @Column(name = "tipo_chave")
    private PixKeyType keyType;

    @Column(name = "chave_mascarada")
    private String maskedKey;

    @Column(
        name = "hash_chave",
        length = 64
    )
    private String keyHash;

    @Column(name = "nome_destinatario")
    private String recipientName;

    @Column(
        name = "documento_destinatario_mascarado"
    )
    private String maskedRecipientDocument;

    @Column(name = "codigo_banco_destino")
    private String destinationBankCode;

    @Column(name = "nome_banco_destino")
    private String destinationBankName;

    @Column(name = "valor")
    private BigDecimal amount;

    @Convert(converter = PixStatusConverter.class)
    @Column(name = "situacao")
    private PixStatus status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "chave_idempotencia",
        columnDefinition = "CHAR(36)"
    )
    private UUID idempotencyKey;

    @Column(
        name = "hash_requisicao",
        length = 64
    )
    private String requestFingerprint;

    @Column(name = "solicitada_em")
    private Instant requestedAt;

    @Column(name = "agendada_para")
    private LocalDate scheduledFor;

    @Column(name = "processada_em")
    private Instant processedAt;

    @Column(name = "referencia_liquidacao")
    private String settlementReference;

    @Column(name = "codigo_falha")
    private String failureCode;

    @Column(name = "mensagem_falha")
    private String failureMessageKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "recorrencia_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID recurrenceId;

    @Convert(converter = PixFrequencyConverter.class)
    @Column(name = "frequencia_recorrencia")
    private PixFrequency frequency;

    @Column(name = "numero_ocorrencia")
    private Integer occurrenceNumber;

    @Column(name = "total_ocorrencias")
    private Integer totalOccurrences;

    protected PixPaymentData() {
    }

    public PixPaymentData(
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