package br.com.spbank.transferencia.adapter.out.persistence.mysql.data;

import br.com.spbank.transferencia.adapter.out.persistence.mysql.converter.*;
import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.transferencia.adapter.out.persistence.mysql.converter.TransferStatusConverter;
import br.com.spbank.transferencia.adapter.out.persistence.mysql.converter.TransferTypeConverter;
import br.com.spbank.transferencia.application.model.TransferStatus;
import br.com.spbank.transferencia.application.model.TransferType;

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
@Table(name = "transferencias")
public class TransferData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Convert(converter = TransferTypeConverter.class)
    @Column(name = "tipo_transferencia")
    private TransferType type;

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

    @Column(name = "nome_destinatario")
    private String recipientName;

    @Column(name = "documento_destinatario")
    private String recipientDocument;

    @Column(name = "codigo_banco_destino")
    private String recipientBankCode;

    @Column(name = "agencia_destino")
    private String recipientBranch;

    @Column(name = "numero_conta_destino")
    private String recipientAccountNumber;

    @Convert(converter = TransferAccountTypeConverter.class)
    @Column(name = "tipo_conta_destino")
    private AccountType recipientAccountType;

    @Column(name = "valor")
    private BigDecimal amount;

    @Column(name = "taxa")
    private BigDecimal fee;

    @Column(name = "taxa_calculada")
    private boolean feeCalculated;

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

    @Convert(converter = TransferStatusConverter.class)
    @Column(name = "situacao")
    private TransferStatus status;

    @Column(name = "solicitada_em")
    private Instant requestedAt;

    @Column(name = "agendada_para")
    private LocalDate scheduledFor;

    @Column(name = "processada_em")
    private Instant processedAt;

    @Column(name = "codigo_falha")
    private String failureCode;

    @Column(name = "mensagem_falha")
    private String failureReason;

    @Column(name = "referencia_liquidacao")
    private String settlementReference;

    protected TransferData() {
    }

    public TransferData(
            UUID id,
            TransferType type,
            UUID sourceAccountId,
            UUID targetAccountId,
            String recipientName,
            String recipientDocument,
            String recipientBankCode,
            String recipientBranch,
            String recipientAccountNumber,
            AccountType recipientAccountType,
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
            String failureReason,
            String settlementReference
    ) {
        this.id = id;
        this.type = type;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.recipientName = recipientName;
        this.recipientDocument = recipientDocument;
        this.recipientBankCode = recipientBankCode;
        this.recipientBranch = recipientBranch;
        this.recipientAccountNumber = recipientAccountNumber;
        this.recipientAccountType = recipientAccountType;
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
        this.failureReason = failureReason;
        this.settlementReference = settlementReference;
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

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientDocument() {
        return recipientDocument;
    }

    public String getRecipientBankCode() {
        return recipientBankCode;
    }

    public String getRecipientBranch() {
        return recipientBranch;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public AccountType getRecipientAccountType() {
        return recipientAccountType;
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

    public String getFailureReason() {
        return failureReason;
    }

    public String getSettlementReference() {
        return settlementReference;
    }
}