package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import br.com.spbank.cartao.application.model.finance.CardPaymentPreference;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="preferencias_pagamento_cartao")
public class CardPaymentPreferenceData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="account_id",columnDefinition="CHAR(36)") private UUID accountId;
    protected CardPaymentPreferenceData() {}
    public CardPaymentPreferenceData(CardPaymentPreference row) {
        id=row.id();
        customerId=row.customerId();
        accountId=row.accountId();
    }
    public CardPaymentPreference toDomain() {return new CardPaymentPreference(id, customerId, accountId);}
}
