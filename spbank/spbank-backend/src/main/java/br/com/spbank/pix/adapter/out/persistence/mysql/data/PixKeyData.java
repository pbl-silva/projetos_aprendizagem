package br.com.spbank.pix.adapter.out.persistence.mysql.data;

import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixKeyTypeConverter;
import br.com.spbank.pix.application.model.PixKeyType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chaves_pix")
public class PixKeyData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        columnDefinition = "CHAR(36)"
    )
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "conta_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID accountId;

    @Convert(
        converter = PixKeyTypeConverter.class
    )
    @Column(name = "tipo_chave")
    private PixKeyType type;

    @Column(
        name = "valor_normalizado"
    )
    private String normalizedValue;

    @Column(name = "ativa")
    private boolean active;

    @Column(name = "criada_em")
    private Instant createdAt;

    protected PixKeyData() {
    }

    public PixKeyData(
        UUID id,
        UUID accountId,
        PixKeyType type,
        String normalizedValue,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.normalizedValue = normalizedValue;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public PixKeyType getType() {
        return type;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}