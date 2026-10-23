package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="posicoes_investimento")
public class InvestmentPositionData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="application_id",columnDefinition="CHAR(36)") public UUID applicationId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="customer_id",columnDefinition="CHAR(36)") public UUID customerId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="account_id",columnDefinition="CHAR(36)") public UUID accountId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="product_id",columnDefinition="CHAR(36)") public UUID productId;
    @Column(name="type",columnDefinition="VARCHAR(240)") public String type;
    @Column(name="terms_json",columnDefinition="LONGTEXT") public String termsJson;
    @Column(name="principal",columnDefinition="DECIMAL(19,2)") public BigDecimal principal;
    @Column(name="effective_date",columnDefinition="DATE") public LocalDate effectiveDate;
    @Column(name="units",columnDefinition="DECIMAL(27,12)") public BigDecimal units;
    @Column(name="unit_price",columnDefinition="DECIMAL(27,12)") public BigDecimal unitPrice;
    @Column(name="quote_date",columnDefinition="DATE") public LocalDate quoteDate;
    @Column(name="maturity_date",columnDefinition="DATE") public LocalDate maturityDate;
    public InvestmentPositionData() {}
}
