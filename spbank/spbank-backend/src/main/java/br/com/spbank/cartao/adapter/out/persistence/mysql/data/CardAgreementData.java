package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardAgreement;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="acordos_cartao")
public class CardAgreementData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="proposal_id",columnDefinition="CHAR(36)") private UUID proposalId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="previous_agreement_id",columnDefinition="CHAR(36)") private UUID previousAgreementId;
    @Column(name="principal",columnDefinition="DECIMAL(19,2)") private BigDecimal principal;
    @Column(name="interest",columnDefinition="DECIMAL(19,2)") private BigDecimal interest;
    @Column(name="rate",columnDefinition="DECIMAL(27,8)") private BigDecimal rate;
    @Column(name="installments",columnDefinition="INT") private int installments;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="plan",columnDefinition="VARCHAR(240)") private CustomerPlan plan;
    @Column(name="status",columnDefinition="VARCHAR(240)") private String status;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardAgreementData() {}
    public CardAgreementData(CardAgreement row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        proposalId=row.proposalId();
        previousAgreementId=row.previousAgreementId();
        principal=row.principal();
        interest=row.interest();
        rate=row.rate();
        installments=row.installments();
        plan=row.plan();
        status=row.status();
        createdAt=row.createdAt();
    }
    public CardAgreement toDomain() {return new CardAgreement(id, customerId, brand, proposalId, previousAgreementId, principal, interest, rate, installments, plan, status, createdAt);}
}
