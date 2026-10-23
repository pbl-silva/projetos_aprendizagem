package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardInvoice;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="faturas_cartao")
public class CardInvoiceData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @Column(name="referencia",columnDefinition="VARCHAR(240)") private String reference;
    @Column(name="nominal_due",columnDefinition="DATE") private LocalDate nominalDue;
    @Column(name="effective_due",columnDefinition="DATE") private LocalDate effectiveDue;
    @Column(name="closing_at",columnDefinition="DATETIME(6)") private Instant closingAt;
    @Column(name="closed",columnDefinition="BOOLEAN") private boolean closed;
    @Column(name="billed_total",columnDefinition="DECIMAL(19,2)") private BigDecimal billedTotal;
    protected CardInvoiceData() {}
    public CardInvoiceData(CardInvoice row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        reference=row.reference();
        nominalDue=row.nominalDue();
        effectiveDue=row.effectiveDue();
        closingAt=row.closingAt();
        closed=row.closed();
        billedTotal=row.billedTotal();
    }
    public CardInvoice toDomain() {return new CardInvoice(id, customerId, brand, reference, nominalDue, effectiveDue, closingAt, closed, billedTotal);}
}
