package br.com.spbank.investimento.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="resgates_investimento")
public class InvestmentRedemptionData {
 @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length=36) public UUID id;
 @JdbcTypeCode(SqlTypes.CHAR) @Column(name="cliente_id",length=36) public UUID customerId;
 @Column(name="dados",columnDefinition="json") public String payload;
}
