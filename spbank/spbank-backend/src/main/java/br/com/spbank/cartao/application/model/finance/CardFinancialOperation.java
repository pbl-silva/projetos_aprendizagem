package br.com.spbank.cartao.application.model.finance;
import java.util.UUID;

public record CardFinancialOperation(UUID id, UUID customerId, UUID key, String fingerprint, String responseJson) implements CardFinanceRecord {
    public CardFinancialOperation withKey(UUID value) { return new CardFinancialOperation(id, customerId, value, fingerprint, responseJson); }
    public CardFinancialOperation withFingerprint(String value) { return new CardFinancialOperation(id, customerId, key, value, responseJson); }
    public CardFinancialOperation withResponseJson(String value) { return new CardFinancialOperation(id, customerId, key, fingerprint, value); }
}
