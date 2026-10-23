package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardProposal;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="propostas_acordo_cartao")
public class CardProposalData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @Column(name="covered_ids",columnDefinition="LONGTEXT") private String coveredIds;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="previous_agreement_id",columnDefinition="CHAR(36)") private UUID previousAgreementId;
    @Column(name="debt",columnDefinition="DECIMAL(19,2)") private BigDecimal debt;
    @Column(name="rate",columnDefinition="DECIMAL(27,8)") private BigDecimal rate;
    @Column(name="interest",columnDefinition="DECIMAL(19,2)") private BigDecimal interest;
    @Column(name="entry",columnDefinition="DECIMAL(19,2)") private BigDecimal entry;
    @Column(name="total",columnDefinition="DECIMAL(19,2)") private BigDecimal total;
    @Column(name="installments",columnDefinition="INT") private int installments;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="plan",columnDefinition="VARCHAR(240)") private CustomerPlan plan;
    @Column(name="valid_on",columnDefinition="DATE") private LocalDate validOn;
    @Column(name="status",columnDefinition="VARCHAR(240)") private String status;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardProposalData() {}
    public CardProposalData(CardProposal row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        coveredIds=row.coveredIds();
        previousAgreementId=row.previousAgreementId();
        debt=row.debt();
        rate=row.rate();
        interest=row.interest();
        entry=row.entry();
        total=row.total();
        installments=row.installments();
        plan=row.plan();
        validOn=row.validOn();
        status=row.status();
        createdAt=row.createdAt();
    }
    public CardProposal toDomain() {return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, interest, entry, total, installments, plan, validOn, status, createdAt);}
}
