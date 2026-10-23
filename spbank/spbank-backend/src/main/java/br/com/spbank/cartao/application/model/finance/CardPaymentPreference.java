package br.com.spbank.cartao.application.model.finance;
import java.util.UUID;

public record CardPaymentPreference(UUID id, UUID customerId, UUID accountId) implements CardFinanceRecord {
    public CardPaymentPreference withAccountId(UUID value) { return new CardPaymentPreference(id, customerId, value); }
}
