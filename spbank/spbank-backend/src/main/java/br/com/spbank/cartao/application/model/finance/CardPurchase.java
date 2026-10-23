package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardPurchase(UUID id, UUID customerId, CardBrand brand, UUID cardId, String merchant, BigDecimal principal, BigDecimal interest, BigDecimal refundedPrincipal, BigDecimal refundedInterest, BigDecimal discountedInterest, CustomerPlan plan, int installments, Instant createdAt) implements CardFinanceRecord {
    public CardPurchase withCardId(UUID value) { return new CardPurchase(id, customerId, brand, value, merchant, principal, interest, refundedPrincipal, refundedInterest, discountedInterest, plan, installments, createdAt); }
    public CardPurchase withMerchant(String value) { return new CardPurchase(id, customerId, brand, cardId, value, principal, interest, refundedPrincipal, refundedInterest, discountedInterest, plan, installments, createdAt); }
    public CardPurchase withPrincipal(BigDecimal value) { return new CardPurchase(id, customerId, brand, cardId, merchant, value, interest, refundedPrincipal, refundedInterest, discountedInterest, plan, installments, createdAt); }
    public CardPurchase withInterest(BigDecimal value) { return new CardPurchase(id, customerId, brand, cardId, merchant, principal, value, refundedPrincipal, refundedInterest, discountedInterest, plan, installments, createdAt); }
    public CardPurchase withRefundedPrincipal(BigDecimal value) { return new CardPurchase(id, customerId, brand, cardId, merchant, principal, interest, value, refundedInterest, discountedInterest, plan, installments, createdAt); }
    public CardPurchase withRefundedInterest(BigDecimal value) { return new CardPurchase(id, customerId, brand, cardId, merchant, principal, interest, refundedPrincipal, value, discountedInterest, plan, installments, createdAt); }
    public CardPurchase withDiscountedInterest(BigDecimal value) { return new CardPurchase(id, customerId, brand, cardId, merchant, principal, interest, refundedPrincipal, refundedInterest, value, plan, installments, createdAt); }
    public CardPurchase withPlan(CustomerPlan value) { return new CardPurchase(id, customerId, brand, cardId, merchant, principal, interest, refundedPrincipal, refundedInterest, discountedInterest, value, installments, createdAt); }
    public CardPurchase withInstallments(int value) { return new CardPurchase(id, customerId, brand, cardId, merchant, principal, interest, refundedPrincipal, refundedInterest, discountedInterest, plan, value, createdAt); }
}
