package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.util.UUID;

public record BillingLine(UUID id, UUID customerId, CardBrand brand, int dueDay, int pendingDueDay, String effectiveReference, String firstReference, BigDecimal creditBalance) implements CardFinanceRecord {
    public BillingLine withDueDay(int value) { return new BillingLine(id, customerId, brand, value, pendingDueDay, effectiveReference, firstReference, creditBalance); }
    public BillingLine withPendingDueDay(int value) { return new BillingLine(id, customerId, brand, dueDay, value, effectiveReference, firstReference, creditBalance); }
    public BillingLine withEffectiveReference(String value) { return new BillingLine(id, customerId, brand, dueDay, pendingDueDay, value, firstReference, creditBalance); }
    public BillingLine withFirstReference(String value) { return new BillingLine(id, customerId, brand, dueDay, pendingDueDay, effectiveReference, value, creditBalance); }
    public BillingLine withCreditBalance(BigDecimal value) { return new BillingLine(id, customerId, brand, dueDay, pendingDueDay, effectiveReference, firstReference, value); }
}
