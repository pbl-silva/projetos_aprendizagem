package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="cotas_investimento")
public class InvestmentQuoteData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") public UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="product_id",columnDefinition="CHAR(36)") public UUID productId;
    @Column(name="quote_date",columnDefinition="DATE") public LocalDate quoteDate;
    @Column(name="price",columnDefinition="DECIMAL(27,12)") public BigDecimal price;
    public InvestmentQuoteData() {}
}
