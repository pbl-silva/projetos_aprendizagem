
package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import br.com.spbank.cartao.application.model.CreditRequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "solicitacoes_credito")
public class CreditRequestData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "id",
            columnDefinition = "CHAR(36)"
    )
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "cliente_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID customerId;

    @Column(
            name = "total_solicitado",
            precision = 19,
            scale = 2
    )
    private BigDecimal requestedTotal;

    @Column(
            name = "motivo",
            length = 240
    )
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "situacao",
            length = 20
    )
    private CreditRequestStatus status;

    @Column(
            name = "total_ofertado",
            precision = 19,
            scale = 2
    )
    private BigDecimal offeredTotal;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "administrador_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID administratorId;

    @Column(
            name = "motivo_decisao",
            length = 240
    )
    private String decisionReason;

    @Column(name = "criada_em")
    private Instant createdAt;

    @Column(name = "decidida_em")
    private Instant decidedAt;

    @Column(name = "expira_em")
    private Instant expiresAt;

    @Column(name = "encerrada_em")
    private Instant closedAt;

    protected CreditRequestData() {
    }

    public CreditRequestData(
            UUID id,
            UUID customerId,
            BigDecimal requestedTotal,
            String reason,
            CreditRequestStatus status,
            BigDecimal offeredTotal,
            UUID administratorId,
            String decisionReason,
            Instant createdAt,
            Instant decidedAt,
            Instant expiresAt,
            Instant closedAt
    ) {
        this.id = id;
        this.customerId = customerId;
        this.requestedTotal = requestedTotal;
        this.reason = reason;
        this.status = status;
        this.offeredTotal = offeredTotal;
        this.administratorId = administratorId;
        this.decisionReason = decisionReason;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
        this.expiresAt = expiresAt;
        this.closedAt = closedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getRequestedTotal() {
        return requestedTotal;
    }

    public String getReason() {
        return reason;
    }

    public CreditRequestStatus getStatus() {
        return status;
    }

    public BigDecimal getOfferedTotal() {
        return offeredTotal;
    }

    public UUID getAdministratorId() {
        return administratorId;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
