package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardSettlement;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="alocacoes_pagamento_cartao")
public class CardSettlementData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="payment_id",columnDefinition="CHAR(36)") private UUID paymentId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="obligation_id",columnDefinition="CHAR(36)") private UUID obligationId;
    @Column(name="kind",columnDefinition="VARCHAR(240)") private String kind;
    @Column(name="interest_paid",columnDefinition="DECIMAL(19,2)") private BigDecimal interestPaid;
    @Column(name="principal_paid",columnDefinition="DECIMAL(19,2)") private BigDecimal principalPaid;
    @Column(name="fine_paid",columnDefinition="DECIMAL(19,2)") private BigDecimal finePaid;
    @Column(name="mora_paid",columnDefinition="DECIMAL(19,2)") private BigDecimal moraPaid;
    @Column(name="discount",columnDefinition="DECIMAL(19,2)") private BigDecimal discount;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardSettlementData() {}
    public CardSettlementData(CardSettlement row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        paymentId=row.paymentId();
        obligationId=row.obligationId();
        kind=row.kind();
        interestPaid=row.interestPaid();
        principalPaid=row.principalPaid();
        finePaid=row.finePaid();
        moraPaid=row.moraPaid();
        discount=row.discount();
        createdAt=row.createdAt();
    }
    public CardSettlement toDomain() {return new CardSettlement(id, customerId, brand, paymentId, obligationId, kind, interestPaid, principalPaid, finePaid, moraPaid, discount, createdAt);}
}
