package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "operacoes_distribuicao_credito")
public class CreditAllocationOperationData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cliente_id", columnDefinition = "CHAR(36)", nullable = false)
    private UUID customerId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "chave_idempotencia", columnDefinition = "CHAR(36)", nullable = false)
    private UUID key;

    @Column(name = "hash_requisicao", length = 64, nullable = false)
    private String fingerprint;

    @Column(name = "resposta_json", columnDefinition = "LONGTEXT", nullable = false)
    private String resultJson;

    protected CreditAllocationOperationData() {
    }

    public CreditAllocationOperationData(UUID id, UUID customerId, UUID key,
            String fingerprint, String resultJson) {
        this.id = id;
        this.customerId = customerId;
        this.key = key;
        this.fingerprint = fingerprint;
        this.resultJson = resultJson;
    }

    public UUID getCustomerId() { return customerId; }
    public UUID getKey() { return key; }
    public String getFingerprint() { return fingerprint; }
    public String getResultJson() { return resultJson; }
}
