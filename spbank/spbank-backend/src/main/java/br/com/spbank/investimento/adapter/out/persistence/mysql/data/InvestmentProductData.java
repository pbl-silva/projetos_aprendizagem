package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="produtos_investimento")
public class InvestmentProductData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @Column(name="name",columnDefinition="VARCHAR(240)") public String name;
    @Column(name="type",columnDefinition="VARCHAR(240)") public String type;
    @Column(name="minimum_application",columnDefinition="DECIMAL(19,2)") public BigDecimal minimumApplication;
    @Column(name="minimum_remaining",columnDefinition="DECIMAL(19,2)") public BigDecimal minimumRemaining;
    @Column(name="risk",columnDefinition="VARCHAR(240)") public String risk;
    @Column(name="active",columnDefinition="BIT(1)") public boolean active;
    @Column(name="application_days",columnDefinition="INT") public Integer applicationDays;
    @Column(name="redemption_days",columnDefinition="INT") public Integer redemptionDays;
    @Column(name="payment_days",columnDefinition="INT") public Integer paymentDays;
    @Column(name="cdi_percent",columnDefinition="DECIMAL(19,8)") public BigDecimal cdiPercent;
    @Column(name="term_days",columnDefinition="INT") public Integer termDays;
    @Column(name="liquidity",columnDefinition="VARCHAR(240)") public String liquidity;
    public InvestmentProductData() {}
}
