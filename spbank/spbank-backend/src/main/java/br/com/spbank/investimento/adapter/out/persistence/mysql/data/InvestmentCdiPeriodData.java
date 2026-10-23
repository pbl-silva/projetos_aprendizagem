package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="fatores_cdi_investimento")
public class InvestmentCdiPeriodData {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @Column(name="valid_from",columnDefinition="DATE") public LocalDate validFrom;
    @Column(name="valid_through",columnDefinition="DATE") public LocalDate validThrough;
    @Column(name="daily_factor",columnDefinition="DECIMAL(27,18)") public BigDecimal dailyFactor;
    @Column(name="business_days_basis",columnDefinition="INT") public int businessDaysBasis;
    @Column(name="source_version",columnDefinition="VARCHAR(80)") public String sourceVersion;
    public InvestmentCdiPeriodData(){}
}
