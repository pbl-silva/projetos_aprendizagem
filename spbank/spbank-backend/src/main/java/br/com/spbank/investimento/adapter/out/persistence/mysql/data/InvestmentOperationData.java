package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="operacoes_investimento")
public class InvestmentOperationData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="customer_id",columnDefinition="CHAR(36)") public UUID customerId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="operation_key",columnDefinition="CHAR(36)") public UUID operationKey;
    @Column(name="fingerprint",columnDefinition="CHAR(64)") public String fingerprint;
    @Column(name="response_json",columnDefinition="LONGTEXT") public String responseJson;
    @Column(name="created_at",columnDefinition="DATETIME(6)") public Instant createdAt;
    public InvestmentOperationData() {}
}
