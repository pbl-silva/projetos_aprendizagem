package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardRefundRequest(UUID id, UUID customerId, CardBrand brand, UUID purchaseId, BigDecimal principal, String reason, String status, UUID administratorId, String decisionReason, Instant createdAt) implements CardFinanceRecord {
    public CardRefundRequest withPurchaseId(UUID value) { return new CardRefundRequest(id, customerId, brand, value, principal, reason, status, administratorId, decisionReason, createdAt); }
    public CardRefundRequest withPrincipal(BigDecimal value) { return new CardRefundRequest(id, customerId, brand, purchaseId, value, reason, status, administratorId, decisionReason, createdAt); }
    public CardRefundRequest withReason(String value) { return new CardRefundRequest(id, customerId, brand, purchaseId, principal, value, status, administratorId, decisionReason, createdAt); }
    public CardRefundRequest withStatus(String value) { return new CardRefundRequest(id, customerId, brand, purchaseId, principal, reason, value, administratorId, decisionReason, createdAt); }
    public CardRefundRequest withAdministratorId(UUID value) { return new CardRefundRequest(id, customerId, brand, purchaseId, principal, reason, status, value, decisionReason, createdAt); }
    public CardRefundRequest withDecisionReason(String value) { return new CardRefundRequest(id, customerId, brand, purchaseId, principal, reason, status, administratorId, value, createdAt); }
}
