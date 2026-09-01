package br.com.spbank.autenticacao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sessoes_acesso")
public class AccessSessionData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "cliente_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID customerId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "conta_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID accountId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "criada_em")
    private Instant createdAt;

    @Column(name = "expira_em")
    private Instant expiresAt;

    @Column(name = "revogada_em")
    private Instant revokedAt;

    protected AccessSessionData() {
    }

    public AccessSessionData(
            UUID id,
            UUID customerId,
            UUID accountId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant revokedAt
    ) {
        this.id = id;
        this.customerId = customerId;
        this.accountId = accountId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(
            UUID accountId
    ) {
        this.accountId = accountId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}