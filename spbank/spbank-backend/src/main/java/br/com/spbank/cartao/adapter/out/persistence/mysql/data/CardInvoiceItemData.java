package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardInvoiceItem;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="itens_fatura_cartao")
public class CardInvoiceItemData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="invoice_id",columnDefinition="CHAR(36)") private UUID invoiceId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="obligation_id",columnDefinition="CHAR(36)") private UUID obligationId;
    @Column(name="source_type",columnDefinition="VARCHAR(240)") private String sourceType;
    @Column(name="original_principal",columnDefinition="DECIMAL(19,2)") private BigDecimal originalPrincipal;
    @Column(name="original_interest",columnDefinition="DECIMAL(19,2)") private BigDecimal originalInterest;
    protected CardInvoiceItemData() {}
    public CardInvoiceItemData(CardInvoiceItem row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        invoiceId=row.invoiceId();
        obligationId=row.obligationId();
        sourceType=row.sourceType();
        originalPrincipal=row.originalPrincipal();
        originalInterest=row.originalInterest();
    }
    public CardInvoiceItem toDomain() {return new CardInvoiceItem(id, customerId, brand, invoiceId, obligationId, sourceType, originalPrincipal, originalInterest);}
}
