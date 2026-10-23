package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="aplicacoes_investimento")
public class InvestmentApplicationData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="customer_id",columnDefinition="CHAR(36)") public UUID customerId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="account_id",columnDefinition="CHAR(36)") public UUID accountId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="product_id",columnDefinition="CHAR(36)") public UUID productId;
    @Column(name="amount",columnDefinition="DECIMAL(19,2)") public BigDecimal amount;
    @Column(name="effective_date",columnDefinition="DATE") public LocalDate effectiveDate;
    @Column(name="status",columnDefinition="VARCHAR(240)") public String status;
    @Column(name="requested_at",columnDefinition="DATETIME(6)") public Instant requestedAt;
    @Column(name="processed_at",columnDefinition="DATETIME(6)") public Instant processedAt;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="position_id",columnDefinition="CHAR(36)") public UUID positionId;
    @Column(name="balance_after",columnDefinition="DECIMAL(19,2)") public BigDecimal balanceAfter;
    @Column(name="failure_code",columnDefinition="VARCHAR(240)") public String failureCode;
    public InvestmentApplicationData() {}
}
