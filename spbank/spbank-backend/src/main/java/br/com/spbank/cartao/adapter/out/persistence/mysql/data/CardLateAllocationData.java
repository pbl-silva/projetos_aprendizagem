package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.finance.CardLateAllocation;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="alocacoes_encargo_atraso_cartao") public class CardLateAllocationData {
@Id @JdbcTypeCode(SqlTypes.CHAR) @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
@JdbcTypeCode(SqlTypes.CHAR) @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
@Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
@JdbcTypeCode(SqlTypes.CHAR) @Column(name="payment_id",columnDefinition="CHAR(36)") private UUID paymentId;
@JdbcTypeCode(SqlTypes.CHAR) @Column(name="obligation_id",columnDefinition="CHAR(36)") private UUID obligationId;
@Column(name="fine",columnDefinition="DECIMAL(19,2)") private BigDecimal fine;
@Column(name="mora",columnDefinition="DECIMAL(19,2)") private BigDecimal mora;
@Column(name="created_at",columnDefinition="DATETIME(6)") private Instant createdAt;
protected CardLateAllocationData(){}
public CardLateAllocationData(CardLateAllocation row){id=row.id();customerId=row.customerId();brand=row.brand();paymentId=row.paymentId();obligationId=row.obligationId();fine=row.fine();mora=row.mora();createdAt=row.createdAt();}
public CardLateAllocation toDomain(){return new CardLateAllocation(id,customerId,brand,paymentId,obligationId,fine,mora,createdAt);}
}
