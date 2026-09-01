package br.com.spbank.autenticacao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "credenciais_acesso")
public class AccessCredentialData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "cliente_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID customerId;

    @Column(name = "usuario")
    private String username;

    @Column(name = "senha_hash")
    private String passwordHash;

    @Column(name = "ativa")
    private boolean active;

    protected AccessCredentialData() {
    }

    public AccessCredentialData(
            UUID customerId,
            String username,
            String passwordHash,
            boolean active
    ) {
        this.customerId = customerId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }
}