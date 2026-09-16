package br.com.spbank.pix.adapter.out.persistence.mysql.data;

import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixDestinationScopeConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixKeyTypeConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixRecurrenceFrequencyConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixStatusConverter;
import br.com.spbank.pix.application.model.PixDestinationScope;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixRecurrenceFrequency;
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
    @Column(
        columnDefinition = "CHAR(36)"
    )
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "conta_origem_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID originAccountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "conta_destino_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID destinationAccountId;

    @Convert(
        converter =
            PixDestinationScopeConverter.class
    )
    @Column(name = "escopo_destino")
    private PixDestinationScope destinationScope;

    @Convert(
        converter = PixKeyTypeConverter.class
    )
    @Column(name = "tipo_chave")
    private PixKeyType keyType;

    @Column(
        name = "chave_mascarada",
        length = 120
    )
    private String maskedKey;

    @Column(
        name = "hash_chave",
        length = 64
    )
    private String keyHash;

    @Column(
        name = "nome_destinatario",
        length = 120
    )
    private String holderName;

    @Column(
        name = "documento_destinatario_mascarado",
        length = 24
    )
    private String maskedDocument;

    @Column(
        name = "codigo_banco_destino",
        length = 20
    )
    private String bankCode;

    @Column(
        name = "nome_banco_destino",
        length = 120
    )
    private String bankName;

    @Column(name = "valor")
    private BigDecimal amount;

    @Convert(
        converter = PixStatusConverter.class
    )
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
    private String requestHash;

    @Column(name = "solicitada_em")
    private Instant requestedAt;

    @Column(name = "agendada_para")
    private LocalDate scheduledFor;

    @Column(name = "processada_em")
    private Instant processedAt;

    @Column(
        name = "referencia_liquidacao",
        length = 120
    )
    private String settlementReference;

    @Column(
        name = "codigo_falha",
        length = 60
    )
    private String failureCode;

    @Column(
        name = "mensagem_falha",
        length = 240
    )
    private String failureMessage;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "recorrencia_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID recurrenceId;

    @Convert(
        converter =
            PixRecurrenceFrequencyConverter.class
    )
    @Column(name = "frequencia_recorrencia")
    private PixRecurrenceFrequency recurrenceFrequency;

    @Column(name = "numero_ocorrencia")
    private Integer occurrenceNumber;

    @Column(name = "total_ocorrencias")
    private Integer totalOccurrences;

    protected PixPaymentData() {
    }

    public PixPaymentData(
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
        String settlementReference,
        String failureCode,
        String failureMessage,
        UUID recurrenceId,
        PixRecurrenceFrequency recurrenceFrequency,
        Integer occurrenceNumber,
        Integer totalOccurrences
    ) {
        this.id = id;
        this.originAccountId = originAccountId;
        this.destinationAccountId =
            destinationAccountId;
        this.destinationScope =
            destinationScope;
        this.keyType = keyType;
        this.maskedKey = maskedKey;
        this.keyHash = keyHash;
        this.holderName = holderName;
        this.maskedDocument =
            maskedDocument;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey =
            idempotencyKey;
        this.requestHash = requestHash;
        this.requestedAt = requestedAt;
        this.scheduledFor = scheduledFor;
        this.processedAt = processedAt;
        this.settlementReference =
            settlementReference;
        this.failureCode = failureCode;
        this.failureMessage =
            failureMessage;
        this.recurrenceId = recurrenceId;
        this.recurrenceFrequency =
            recurrenceFrequency;
        this.occurrenceNumber =
            occurrenceNumber;
        this.totalOccurrences =
            totalOccurrences;
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

    public String getSettlementReference() {
        return settlementReference;
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
}