package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardAttempt(UUID id, UUID customerId, CardBrand brand, UUID cardId, String lastFour, String merchant, BigDecimal amount, String status, String reason, Instant createdAt) implements CardFinanceRecord {
    public CardAttempt withCardId(UUID value) { return new CardAttempt(id, customerId, brand, value, lastFour, merchant, amount, status, reason, createdAt); }
    public CardAttempt withLastFour(String value) { return new CardAttempt(id, customerId, brand, cardId, value, merchant, amount, status, reason, createdAt); }
    public CardAttempt withMerchant(String value) { return new CardAttempt(id, customerId, brand, cardId, lastFour, value, amount, status, reason, createdAt); }
    public CardAttempt withAmount(BigDecimal value) { return new CardAttempt(id, customerId, brand, cardId, lastFour, merchant, value, status, reason, createdAt); }
    public CardAttempt withStatus(String value) { return new CardAttempt(id, customerId, brand, cardId, lastFour, merchant, amount, value, reason, createdAt); }
    public CardAttempt withReason(String value) { return new CardAttempt(id, customerId, brand, cardId, lastFour, merchant, amount, status, value, createdAt); }
}
