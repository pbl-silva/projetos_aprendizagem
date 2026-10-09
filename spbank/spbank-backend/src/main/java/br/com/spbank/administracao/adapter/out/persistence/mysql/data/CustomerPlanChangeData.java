package br.com.spbank.administracao.adapter.out.persistence.mysql.data;

import br.com.spbank.administracao.adapter.out.persistence.mysql.converter.AdministrativeCustomerPlanConverter;
import br.com.spbank.shared.application.model.CustomerPlan;

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
@Table(name = "alteracoes_plano_cliente")
public class CustomerPlanChangeData {

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
            name = "administrador_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID administratorId;

    @Column(name = "nome_administrador")
    private String administratorName;

    @Convert(
            converter = AdministrativeCustomerPlanConverter.class
    )
    @Column(name = "plano_anterior")
    private CustomerPlan previousPlan;

    @Convert(
            converter = AdministrativeCustomerPlanConverter.class
    )
    @Column(name = "plano_novo")
    private CustomerPlan newPlan;

    @Column(name = "motivo")
    private String reason;

    @Column(name = "alterado_em")
    private Instant changedAt;

    protected CustomerPlanChangeData() {
    }

    public CustomerPlanChangeData(
            UUID id,
            UUID customerId,
            UUID administratorId,
            String administratorName,
            CustomerPlan previousPlan,
            CustomerPlan newPlan,
            String reason,
            Instant changedAt
    ) {
        this.id = id;
        this.customerId = customerId;
        this.administratorId = administratorId;
        this.administratorName = administratorName;
        this.previousPlan = previousPlan;
        this.newPlan = newPlan;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getAdministratorId() {
        return administratorId;
    }

    public String getAdministratorName() {
        return administratorName;
    }

    public CustomerPlan getPreviousPlan() {
        return previousPlan;
    }

    public CustomerPlan getNewPlan() {
        return newPlan;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}