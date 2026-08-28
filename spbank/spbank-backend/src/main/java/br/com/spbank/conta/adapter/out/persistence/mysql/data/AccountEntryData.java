package br.com.spbank.conta.adapter.out.persistence.mysql.data;

import br.com.spbank.conta.adapter.out.persistence.mysql.converter.EntryDirectionConverter;
import br.com.spbank.conta.adapter.out.persistence.mysql.converter.EntryReferenceTypeConverter;
import br.com.spbank.conta.adapter.out.persistence.mysql.converter.EntryTypeConverter;
import br.com.spbank.conta.application.model.EntryDirection;
import br.com.spbank.conta.application.model.EntryReferenceType;
import br.com.spbank.conta.application.model.EntryType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lancamentos_conta")
public class AccountEntryData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "conta_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "referencia_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID referenceId;

    @Convert(converter = EntryReferenceTypeConverter.class)
    @Column(name = "tipo_referencia")
    private EntryReferenceType referenceType;

    @Convert(converter = EntryTypeConverter.class)
    @Column(name = "tipo_lancamento")
    private EntryType type;

    @Convert(converter = EntryDirectionConverter.class)
    @Column(name = "natureza")
    private EntryDirection direction;

    @Column(name = "valor")
    private BigDecimal amount;

    @Column(name = "saldo_apos")
    private BigDecimal balanceAfter;

    @Column(name = "descricao")
    private String description;

    @Column(name = "ocorrido_em")
    private Instant occurredAt;

    @Column(name = "nome_contraparte")
    private String counterpartyName;

    @Column(name = "banco_contraparte")
    private String counterpartyBankCode;

    @Column(name = "modalidade_operacao")
    private String operationType;

    protected AccountEntryData() {
    }

    public AccountEntryData(
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
            String operationType
    ) {
        this.id = id;
        this.accountId = accountId;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.type = type;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.occurredAt = occurredAt;
        this.counterpartyName = counterpartyName;
        this.counterpartyBankCode = counterpartyBankCode;
        this.operationType = operationType;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public EntryReferenceType getReferenceType() {
        return referenceType;
    }

    public EntryType getType() {
        return type;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public String getCounterpartyBankCode() {
        return counterpartyBankCode;
    }

    public String getOperationType() {
        return operationType;
    }
}