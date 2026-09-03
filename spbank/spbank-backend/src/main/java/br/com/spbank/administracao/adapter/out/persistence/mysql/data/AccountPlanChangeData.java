package br.com.spbank.administracao.adapter.out.persistence.mysql.data;

import br.com.spbank.administracao.adapter.out.persistence.mysql.converter.AdministrativeAccountPlanConverter;
import br.com.spbank.conta.application.model.AccountPlan;

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
@Table(name = "alteracoes_plano_conta")
public class AccountPlanChangeData {

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
            name = "administrador_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID administratorId;

    @Column(name = "nome_administrador")
    private String administratorName;

    @Convert(
            converter =
                    AdministrativeAccountPlanConverter.class
    )
    @Column(name = "plano_anterior")
    private AccountPlan previousPlan;

    @Convert(
            converter =
                    AdministrativeAccountPlanConverter.class
    )
    @Column(name = "plano_novo")
    private AccountPlan newPlan;

    @Column(name = "motivo")
    private String reason;

    @Column(name = "alterado_em")
    private Instant changedAt;

    protected AccountPlanChangeData() {
    }

    public AccountPlanChangeData(
            UUID id,
            UUID accountId,
            UUID administratorId,
            String administratorName,
            AccountPlan previousPlan,
            AccountPlan newPlan,
            String reason,
            Instant changedAt
    ) {
        this.id = id;
        this.accountId = accountId;
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

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getAdministratorId() {
        return administratorId;
    }

    public String getAdministratorName() {
        return administratorName;
    }

    public AccountPlan getPreviousPlan() {
        return previousPlan;
    }

    public AccountPlan getNewPlan() {
        return newPlan;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}