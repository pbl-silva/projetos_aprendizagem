package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import br.com.spbank.cartao.application.model.CardBrand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "linhas_credito_cartao")
@IdClass(CardCreditLineId.class)
public class CardCreditLineData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cliente_id", columnDefinition = "CHAR(36)", nullable = false)
    private UUID customerId;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "bandeira", length = 20, nullable = false)
    private CardBrand brand;

    @Column(name = "limite_alocado", precision = 19, scale = 2, nullable = false)
    private BigDecimal allocated;

    @Column(name = "valor_comprometido", precision = 19, scale = 2, nullable = false)
    private BigDecimal committed;

    protected CardCreditLineData() {
    }

    public CardCreditLineData(UUID customerId, CardBrand brand,
            BigDecimal allocated, BigDecimal committed) {
        this.customerId = customerId;
        this.brand = brand;
        this.allocated = allocated;
        this.committed = committed;
    }

    public UUID getCustomerId() { return customerId; }
    public CardBrand getBrand() { return brand; }
    public BigDecimal getAllocated() { return allocated; }
    public BigDecimal getCommitted() { return committed; }
}
