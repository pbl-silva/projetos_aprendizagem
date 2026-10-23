package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="feriados_investimento")
public class InvestmentHolidayData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @Column(name="month_day",columnDefinition="VARCHAR(240)") public String monthDay;
    @Column(name="exact_date",columnDefinition="DATE") public LocalDate exactDate;
    public InvestmentHolidayData() {}
}
