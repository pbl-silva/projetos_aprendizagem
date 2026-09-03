package br.com.spbank.administracao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sessoes_administrativas")
public class AdministratorSessionData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "administrador_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID administratorId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "criada_em")
    private Instant createdAt;

    @Column(name = "expira_em")
    private Instant expiresAt;

    @Column(name = "revogada_em")
    private Instant revokedAt;

    protected AdministratorSessionData() {
    }

    public AdministratorSessionData(
            UUID id,
            UUID administratorId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant revokedAt
    ) {
        this.id = id;
        this.administratorId = administratorId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAdministratorId() {
        return administratorId;
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