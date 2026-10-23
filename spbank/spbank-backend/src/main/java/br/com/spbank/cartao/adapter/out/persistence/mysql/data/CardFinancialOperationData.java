package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardFinancialOperation;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="operacoes_cartao")
public class CardFinancialOperationData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="chave_idempotencia",columnDefinition="CHAR(36)") private UUID key;
    @Column(name="fingerprint",columnDefinition="VARCHAR(240)") private String fingerprint;
    @Column(name="response_json",columnDefinition="LONGTEXT") private String responseJson;
    protected CardFinancialOperationData() {}
    public CardFinancialOperationData(CardFinancialOperation row) {
        id=row.id();
        customerId=row.customerId();
        key=row.key();
        fingerprint=row.fingerprint();
        responseJson=row.responseJson();
    }
    public CardFinancialOperation toDomain() {return new CardFinancialOperation(id, customerId, key, fingerprint, responseJson);}
}
