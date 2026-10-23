package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.util.UUID;

public record CardInvoiceItem(UUID id, UUID customerId, CardBrand brand, UUID invoiceId, UUID obligationId, String sourceType, BigDecimal originalPrincipal, BigDecimal originalInterest) implements CardFinanceRecord {
    public CardInvoiceItem withInvoiceId(UUID value) { return new CardInvoiceItem(id, customerId, brand, value, obligationId, sourceType, originalPrincipal, originalInterest); }
    public CardInvoiceItem withObligationId(UUID value) { return new CardInvoiceItem(id, customerId, brand, invoiceId, value, sourceType, originalPrincipal, originalInterest); }
    public CardInvoiceItem withSourceType(String value) { return new CardInvoiceItem(id, customerId, brand, invoiceId, obligationId, value, originalPrincipal, originalInterest); }
    public CardInvoiceItem withOriginalPrincipal(BigDecimal value) { return new CardInvoiceItem(id, customerId, brand, invoiceId, obligationId, sourceType, value, originalInterest); }
    public CardInvoiceItem withOriginalInterest(BigDecimal value) { return new CardInvoiceItem(id, customerId, brand, invoiceId, obligationId, sourceType, originalPrincipal, value); }
}
