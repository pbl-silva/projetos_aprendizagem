package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardSettlement(UUID id, UUID customerId, CardBrand brand, UUID paymentId, UUID obligationId, String kind, BigDecimal interestPaid, BigDecimal principalPaid, BigDecimal finePaid, BigDecimal moraPaid, BigDecimal discount, Instant createdAt) implements CardFinanceRecord {
    public CardSettlement withPaymentId(UUID value) { return new CardSettlement(id, customerId, brand, value, obligationId, kind, interestPaid, principalPaid, finePaid, moraPaid, discount, createdAt); }
    public CardSettlement withObligationId(UUID value) { return new CardSettlement(id, customerId, brand, paymentId, value, kind, interestPaid, principalPaid, finePaid, moraPaid, discount, createdAt); }
    public CardSettlement withKind(String value) { return new CardSettlement(id, customerId, brand, paymentId, obligationId, value, interestPaid, principalPaid, finePaid, moraPaid, discount, createdAt); }
    public CardSettlement withInterestPaid(BigDecimal value) { return new CardSettlement(id, customerId, brand, paymentId, obligationId, kind, value, principalPaid, finePaid, moraPaid, discount, createdAt); }
    public CardSettlement withPrincipalPaid(BigDecimal value) { return new CardSettlement(id, customerId, brand, paymentId, obligationId, kind, interestPaid, value, finePaid, moraPaid, discount, createdAt); }
    public CardSettlement withFinePaid(BigDecimal value) { return new CardSettlement(id, customerId, brand, paymentId, obligationId, kind, interestPaid, principalPaid, value, moraPaid, discount, createdAt); }
    public CardSettlement withMoraPaid(BigDecimal value) { return new CardSettlement(id, customerId, brand, paymentId, obligationId, kind, interestPaid, principalPaid, finePaid, value, discount, createdAt); }
    public CardSettlement withDiscount(BigDecimal value) { return new CardSettlement(id, customerId, brand, paymentId, obligationId, kind, interestPaid, principalPaid, finePaid, moraPaid, value, createdAt); }
}
