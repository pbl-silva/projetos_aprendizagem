package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardObligation;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="parcelas_acordo_cartao")
public class CardAgreementInstallmentData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @Column(name="source_type",columnDefinition="VARCHAR(240)") private String sourceType;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="source_id",columnDefinition="CHAR(36)") private UUID sourceId;
    @Column(name="numero_parcela",columnDefinition="INT") private int sequence;
    @Column(name="referencia",columnDefinition="VARCHAR(240)") private String reference;
    @Column(name="nominal_due",columnDefinition="DATE") private LocalDate nominalDue;
    @Column(name="effective_due",columnDefinition="DATE") private LocalDate effectiveDue;
    @Column(name="closing_at",columnDefinition="DATETIME(6)") private Instant closingAt;
    @Column(name="principal",columnDefinition="DECIMAL(19,2)") private BigDecimal principal;
    @Column(name="interest",columnDefinition="DECIMAL(19,2)") private BigDecimal interest;
    @Column(name="remaining_principal",columnDefinition="DECIMAL(19,2)") private BigDecimal remainingPrincipal;
    @Column(name="remaining_interest",columnDefinition="DECIMAL(19,2)") private BigDecimal remainingInterest;
    @Column(name="status",columnDefinition="VARCHAR(240)") private String status;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="financed_agreement_id",columnDefinition="CHAR(36)") private UUID financedAgreementId;
    protected CardAgreementInstallmentData() {}
    public CardAgreementInstallmentData(CardObligation row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        sourceType=row.sourceType();
        sourceId=row.sourceId();
        sequence=row.sequence();
        reference=row.reference();
        nominalDue=row.nominalDue();
        effectiveDue=row.effectiveDue();
        closingAt=row.closingAt();
        principal=row.principal();
        interest=row.interest();
        remainingPrincipal=row.remainingPrincipal();
        remainingInterest=row.remainingInterest();
        status=row.status();
        financedAgreementId=row.financedAgreementId();
    }
    public CardObligation toDomain() {return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId);}
}
