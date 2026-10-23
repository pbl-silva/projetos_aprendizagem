package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardPayment;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="pagamentos_fatura_cartao")
public class CardPaymentData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @Column(name="purpose",columnDefinition="VARCHAR(240)") private String purpose;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="proposal_id",columnDefinition="CHAR(36)") private UUID proposalId;
    @Column(name="method",columnDefinition="VARCHAR(240)") private String method;
    @Column(name="amount",columnDefinition="DECIMAL(19,2)") private BigDecimal amount;
    @Column(name="applied_amount",columnDefinition="DECIMAL(19,2)") private BigDecimal appliedAmount;
    @Column(name="credited_amount",columnDefinition="DECIMAL(19,2)") private BigDecimal creditedAmount;
    @Column(name="discount",columnDefinition="DECIMAL(19,2)") private BigDecimal discount;
    @Column(name="status",columnDefinition="VARCHAR(240)") private String status;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="account_id",columnDefinition="CHAR(36)") private UUID accountId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="administrator_id",columnDefinition="CHAR(36)") private UUID administratorId;
    @Column(name="reason",columnDefinition="VARCHAR(240)") private String reason;
    @Column(name="code",columnDefinition="VARCHAR(240)") private String code;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    @Column(name="paid_at",columnDefinition="DATETIME(6)") private Instant paidAt;
    protected CardPaymentData() {}
    public CardPaymentData(CardPayment row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        purpose=row.purpose();
        proposalId=row.proposalId();
        method=row.method();
        amount=row.amount();
        appliedAmount=row.appliedAmount();
        creditedAmount=row.creditedAmount();
        discount=row.discount();
        status=row.status();
        accountId=row.accountId();
        administratorId=row.administratorId();
        reason=row.reason();
        code=row.code();
        createdAt=row.createdAt();
        paidAt=row.paidAt();
    }
    public CardPayment toDomain() {return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, discount, status, accountId, administratorId, reason, code, createdAt, paidAt);}
}
