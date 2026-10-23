
package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "operacoes_credito")
public class CreditOperationData {

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

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "solicitacao_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID requestId;

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
    private String fingerprint;

    @Column(
            name = "resposta_json",
            columnDefinition = "LONGTEXT"
    )
    private String resultJson;

    protected CreditOperationData() {
    }

    public CreditOperationData(
            UUID id,
            UUID customerId,
            UUID requestId,
            UUID idempotencyKey,
            String fingerprint,
            String resultJson
    ) {
        this.id = id;
        this.customerId = customerId;
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.fingerprint = fingerprint;
        this.resultJson = resultJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getResultJson() {
        return resultJson;
    }
}
