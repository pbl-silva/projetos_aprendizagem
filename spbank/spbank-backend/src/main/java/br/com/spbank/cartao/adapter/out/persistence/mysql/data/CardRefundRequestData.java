package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardRefundRequest;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="solicitacoes_estorno_cartao")
public class CardRefundRequestData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="purchase_id",columnDefinition="CHAR(36)") private UUID purchaseId;
    @Column(name="principal",columnDefinition="DECIMAL(19,2)") private BigDecimal principal;
    @Column(name="reason",columnDefinition="VARCHAR(240)") private String reason;
    @Column(name="status",columnDefinition="VARCHAR(240)") private String status;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="administrator_id",columnDefinition="CHAR(36)") private UUID administratorId;
    @Column(name="decision_reason",columnDefinition="VARCHAR(240)") private String decisionReason;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardRefundRequestData() {}
    public CardRefundRequestData(CardRefundRequest row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        purchaseId=row.purchaseId();
        principal=row.principal();
        reason=row.reason();
        status=row.status();
        administratorId=row.administratorId();
        decisionReason=row.decisionReason();
        createdAt=row.createdAt();
    }
    public CardRefundRequest toDomain() {return new CardRefundRequest(id, customerId, brand, purchaseId, principal, reason, status, administratorId, decisionReason, createdAt);}
}
