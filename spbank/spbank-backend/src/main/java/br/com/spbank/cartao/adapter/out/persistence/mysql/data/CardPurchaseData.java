package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardPurchase;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="compras_cartao")
public class CardPurchaseData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="card_id",columnDefinition="CHAR(36)") private UUID cardId;
    @Column(name="merchant",columnDefinition="VARCHAR(240)") private String merchant;
    @Column(name="principal",columnDefinition="DECIMAL(19,2)") private BigDecimal principal;
    @Column(name="interest",columnDefinition="DECIMAL(19,2)") private BigDecimal interest;
    @Column(name="refunded_principal",columnDefinition="DECIMAL(19,2)") private BigDecimal refundedPrincipal;
    @Column(name="refunded_interest",columnDefinition="DECIMAL(19,2)") private BigDecimal refundedInterest;
    @Column(name="discounted_interest",columnDefinition="DECIMAL(19,2)") private BigDecimal discountedInterest;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="plan",columnDefinition="VARCHAR(240)") private CustomerPlan plan;
    @Column(name="installments",columnDefinition="INT") private int installments;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardPurchaseData() {}
    public CardPurchaseData(CardPurchase row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        cardId=row.cardId();
        merchant=row.merchant();
        principal=row.principal();
        interest=row.interest();
        refundedPrincipal=row.refundedPrincipal();
        refundedInterest=row.refundedInterest();
        discountedInterest=row.discountedInterest();
        plan=row.plan();
        installments=row.installments();
        createdAt=row.createdAt();
    }
    public CardPurchase toDomain() {return new CardPurchase(id, customerId, brand, cardId, merchant, principal, interest, refundedPrincipal, refundedInterest, discountedInterest, plan, installments, createdAt);}
}
