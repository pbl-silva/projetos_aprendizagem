package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.time.*;
import java.util.UUID;

public record CardDueDayChange(UUID id, UUID customerId, CardBrand brand, int previousDay, int newDay, String effectiveReference, Instant createdAt) implements CardFinanceRecord {
    public CardDueDayChange withPreviousDay(int value) { return new CardDueDayChange(id, customerId, brand, value, newDay, effectiveReference, createdAt); }
    public CardDueDayChange withNewDay(int value) { return new CardDueDayChange(id, customerId, brand, previousDay, value, effectiveReference, createdAt); }
    public CardDueDayChange withEffectiveReference(String value) { return new CardDueDayChange(id, customerId, brand, previousDay, newDay, value, createdAt); }
}
