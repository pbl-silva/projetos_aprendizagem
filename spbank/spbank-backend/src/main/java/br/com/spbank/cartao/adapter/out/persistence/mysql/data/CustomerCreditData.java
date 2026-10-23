
package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "credito_clientes")
public class CustomerCreditData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "cliente_id",
            columnDefinition = "CHAR(36)"
    )
    private UUID customerId;

    @Column(
            name = "total_aprovado",
            precision = 19,
            scale = 2
    )
    private BigDecimal approvedTotal;

    protected CustomerCreditData() {
    }

    public CustomerCreditData(
            UUID customerId,
            BigDecimal approvedTotal
    ) {
        this.customerId = customerId;
        this.approvedTotal = approvedTotal;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getApprovedTotal() {
        return approvedTotal;
    }
}
