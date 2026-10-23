package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardAttempt;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="tentativas_compra_cartao")
public class CardAttemptData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="card_id",columnDefinition="CHAR(36)") private UUID cardId;
    @Column(name="last_four",columnDefinition="VARCHAR(240)") private String lastFour;
    @Column(name="merchant",columnDefinition="VARCHAR(240)") private String merchant;
    @Column(name="amount",columnDefinition="DECIMAL(19,2)") private BigDecimal amount;
    @Column(name="status",columnDefinition="VARCHAR(240)") private String status;
    @Column(name="reason",columnDefinition="VARCHAR(240)") private String reason;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardAttemptData() {}
    public CardAttemptData(CardAttempt row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        cardId=row.cardId();
        lastFour=row.lastFour();
        merchant=row.merchant();
        amount=row.amount();
        status=row.status();
        reason=row.reason();
        createdAt=row.createdAt();
    }
    public CardAttempt toDomain() {return new CardAttempt(id, customerId, brand, cardId, lastFour, merchant, amount, status, reason, createdAt);}
}
