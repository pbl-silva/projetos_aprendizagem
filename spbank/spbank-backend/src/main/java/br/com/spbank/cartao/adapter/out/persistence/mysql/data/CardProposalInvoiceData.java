package br.com.spbank.cartao.adapter.out.persistence.mysql.data;
import jakarta.persistence.*;
import java.util.UUID;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.cartao.application.model.finance.CardProposalInvoice;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="faturas_proposta_acordo_cartao") public class CardProposalInvoiceData {
@Id @JdbcTypeCode(SqlTypes.CHAR) @Column(name="id",columnDefinition="CHAR(36)") private UUID id;
@JdbcTypeCode(SqlTypes.CHAR) @Column(name="cliente_id",columnDefinition="CHAR(36)") private UUID customerId;
@Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name="bandeira",columnDefinition="VARCHAR(240)") private CardBrand brand;
@JdbcTypeCode(SqlTypes.CHAR) @Column(name="proposal_id",columnDefinition="CHAR(36)") private UUID proposalId;
@JdbcTypeCode(SqlTypes.CHAR) @Column(name="invoice_id",columnDefinition="CHAR(36)") private UUID invoiceId;
protected CardProposalInvoiceData(){}
public CardProposalInvoiceData(CardProposalInvoice row){id=row.id();customerId=row.customerId();brand=row.brand();proposalId=row.proposalId();invoiceId=row.invoiceId();}
public CardProposalInvoice toDomain(){return new CardProposalInvoice(id,customerId,brand,proposalId,invoiceId);}
}
