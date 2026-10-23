package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="avaliacoes_investimento")
public class InvestmentValuationData {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name="position_id",columnDefinition="CHAR(36)") public UUID positionId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name="customer_id",columnDefinition="CHAR(36)") public UUID customerId;
    @Column(name="reference_date",columnDefinition="DATE") public LocalDate referenceDate;
    @Column(name="status",columnDefinition="VARCHAR(30)") public String status;
    @Column(name="gross_value",columnDefinition="DECIMAL(19,2)") public BigDecimal grossValue;
    @Column(name="gross_earnings",columnDefinition="DECIMAL(19,2)") public BigDecimal grossEarnings;
    @Column(name="calculated_through",columnDefinition="DATE") public LocalDate calculatedThrough;
    @Column(name="calculation_fingerprint",columnDefinition="CHAR(64)") public String calculationFingerprint;
    @Column(name="algorithm_version",columnDefinition="VARCHAR(30)") public String algorithmVersion;
    @Column(name="response_json",columnDefinition="LONGTEXT") public String responseJson;
    public InvestmentValuationData(){}
}
