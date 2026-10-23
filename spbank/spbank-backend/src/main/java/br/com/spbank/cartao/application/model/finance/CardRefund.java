package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardRefund(UUID id, UUID customerId, CardBrand brand, UUID requestId, UUID purchaseId, BigDecimal principal, BigDecimal interest, BigDecimal credit, Instant createdAt) implements CardFinanceRecord {
    public CardRefund withRequestId(UUID value) { return new CardRefund(id, customerId, brand, value, purchaseId, principal, interest, credit, createdAt); }
    public CardRefund withPurchaseId(UUID value) { return new CardRefund(id, customerId, brand, requestId, value, principal, interest, credit, createdAt); }
    public CardRefund withPrincipal(BigDecimal value) { return new CardRefund(id, customerId, brand, requestId, purchaseId, value, interest, credit, createdAt); }
    public CardRefund withInterest(BigDecimal value) { return new CardRefund(id, customerId, brand, requestId, purchaseId, principal, value, credit, createdAt); }
    public CardRefund withCredit(BigDecimal value) { return new CardRefund(id, customerId, brand, requestId, purchaseId, principal, interest, value, createdAt); }
}
