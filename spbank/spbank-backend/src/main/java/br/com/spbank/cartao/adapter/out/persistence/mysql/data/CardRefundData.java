package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardRefund;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="estornos_cartao")
public class CardRefundData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="request_id",columnDefinition="CHAR(36)") private UUID requestId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="purchase_id",columnDefinition="CHAR(36)") private UUID purchaseId;
    @Column(name="principal",columnDefinition="DECIMAL(19,2)") private BigDecimal principal;
    @Column(name="interest",columnDefinition="DECIMAL(19,2)") private BigDecimal interest;
    @Column(name="credit",columnDefinition="DECIMAL(19,2)") private BigDecimal credit;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardRefundData() {}
    public CardRefundData(CardRefund row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        requestId=row.requestId();
        purchaseId=row.purchaseId();
        principal=row.principal();
        interest=row.interest();
        credit=row.credit();
        createdAt=row.createdAt();
    }
    public CardRefund toDomain() {return new CardRefund(id, customerId, brand, requestId, purchaseId, principal, interest, credit, createdAt);}
}
