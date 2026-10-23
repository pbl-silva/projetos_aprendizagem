package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardDueDayChange;
import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="alteracoes_vencimento_cartao")
public class CardDueDayChangeData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
    @Column(name="previous_day",columnDefinition="INT") private int previousDay;
    @Column(name="new_day",columnDefinition="INT") private int newDay;
    @Column(name="effective_reference",columnDefinition="VARCHAR(240)") private String effectiveReference;
    @Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
    protected CardDueDayChangeData() {}
    public CardDueDayChangeData(CardDueDayChange row) {
        id=row.id();
        customerId=row.customerId();
        brand=row.brand();
        previousDay=row.previousDay();
        newDay=row.newDay();
        effectiveReference=row.effectiveReference();
        createdAt=row.createdAt();
    }
    public CardDueDayChange toDomain() {return new CardDueDayChange(id, customerId, brand, previousDay, newDay, effectiveReference, createdAt);}
}
