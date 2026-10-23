package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardInvoice(UUID id, UUID customerId, CardBrand brand, String reference, LocalDate nominalDue, LocalDate effectiveDue, Instant closingAt, boolean closed, BigDecimal billedTotal) implements CardFinanceRecord {
    public CardInvoice withReference(String value) { return new CardInvoice(id, customerId, brand, value, nominalDue, effectiveDue, closingAt, closed, billedTotal); }
    public CardInvoice withNominalDue(LocalDate value) { return new CardInvoice(id, customerId, brand, reference, value, effectiveDue, closingAt, closed, billedTotal); }
    public CardInvoice withEffectiveDue(LocalDate value) { return new CardInvoice(id, customerId, brand, reference, nominalDue, value, closingAt, closed, billedTotal); }
    public CardInvoice withClosingAt(Instant value) { return new CardInvoice(id, customerId, brand, reference, nominalDue, effectiveDue, value, closed, billedTotal); }
    public CardInvoice withClosed(boolean value) { return new CardInvoice(id, customerId, brand, reference, nominalDue, effectiveDue, closingAt, value, billedTotal); }
    public CardInvoice withBilledTotal(BigDecimal value) { return new CardInvoice(id, customerId, brand, reference, nominalDue, effectiveDue, closingAt, closed, value); }
}
