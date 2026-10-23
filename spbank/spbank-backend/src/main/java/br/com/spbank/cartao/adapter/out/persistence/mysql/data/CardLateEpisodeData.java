package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardLateEpisode;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="episodios_atraso_cartao")
public class CardLateEpisodeData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="obligation_id",columnDefinition="CHAR(36)") private UUID obligationId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="plan",columnDefinition="VARCHAR(240)") private CustomerPlan plan;
    @Column(name="fine_rate",columnDefinition="DECIMAL(27,8)") private BigDecimal fineRate;
    @Column(name="mora_rate",columnDefinition="DECIMAL(27,8)") private BigDecimal moraRate;
    @Column(name="last_processed",columnDefinition="DATE") private LocalDate lastProcessed;
    @Column(name="accrued_raw",columnDefinition="DECIMAL(27,8)") private BigDecimal accruedRaw;
    @Column(name="fine_charged",columnDefinition="DECIMAL(19,2)") private BigDecimal fineCharged;
    @Column(name="mora_charged",columnDefinition="DECIMAL(19,2)") private BigDecimal moraCharged;
    @Column(name="remaining_fine",columnDefinition="DECIMAL(19,2)") private BigDecimal remainingFine;
    @Column(name="remaining_mora",columnDefinition="DECIMAL(19,2)") private BigDecimal remainingMora;
    @Column(name="closed",columnDefinition="BOOLEAN") private boolean closed;
    @Column(name="initial_base",columnDefinition="DECIMAL(19,2)") private BigDecimal initialBase;
    protected CardLateEpisodeData() {}
    public CardLateEpisodeData(CardLateEpisode row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        obligationId=row.obligationId();
        plan=row.plan();
        fineRate=row.fineRate();
        moraRate=row.moraRate();
        initialBase=row.initialBase();
        lastProcessed=row.lastProcessed();
        accruedRaw=row.accruedRaw();
        fineCharged=row.fineCharged();
        moraCharged=row.moraCharged();
        remainingFine=row.remainingFine();
        remainingMora=row.remainingMora();
        closed=row.closed();
    }
    public CardLateEpisode toDomain() {return new CardLateEpisode(id, customerId, brand, obligationId, plan, fineRate, moraRate, initialBase, lastProcessed, accruedRaw, fineCharged, moraCharged, remainingFine, remainingMora, closed);}
}
