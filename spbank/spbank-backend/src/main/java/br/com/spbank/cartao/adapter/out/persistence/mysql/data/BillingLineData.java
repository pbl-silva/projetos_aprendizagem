package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.BillingLine;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="configuracoes_fatura_cartao")
public class BillingLineData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @Column(name="due_day",columnDefinition="INT") private int dueDay;
    @Column(name="pending_due_day",columnDefinition="INT") private int pendingDueDay;
    @Column(name="effective_reference",columnDefinition="VARCHAR(240)") private String effectiveReference;
    @Column(name="first_reference",columnDefinition="VARCHAR(240)") private String firstReference;
    @Column(name="credit_balance",columnDefinition="DECIMAL(19,2)") private BigDecimal creditBalance;
    protected BillingLineData() {}
    public BillingLineData(BillingLine row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        dueDay=row.dueDay();
        pendingDueDay=row.pendingDueDay();
        effectiveReference=row.effectiveReference();
        firstReference=row.firstReference();
        creditBalance=row.creditBalance();
    }
    public BillingLine toDomain() {return new BillingLine(id, customerId, brand, dueDay, pendingDueDay, effectiveReference, firstReference, creditBalance);}
}
