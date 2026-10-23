package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardObligation(UUID id, UUID customerId, CardBrand brand, String sourceType, UUID sourceId, int sequence, String reference, LocalDate nominalDue, LocalDate effectiveDue, Instant closingAt, BigDecimal principal, BigDecimal interest, BigDecimal remainingPrincipal, BigDecimal remainingInterest, String status, UUID financedAgreementId) implements CardFinanceRecord {
    public CardObligation withSourceType(String value) { return new CardObligation(id, customerId, brand, value, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withSourceId(UUID value) { return new CardObligation(id, customerId, brand, sourceType, value, sequence, reference, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withSequence(int value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, value, reference, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withReference(String value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, value, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withNominalDue(LocalDate value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, value, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withEffectiveDue(LocalDate value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, value, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withClosingAt(Instant value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, value, principal, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withPrincipal(BigDecimal value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, value, interest, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withInterest(BigDecimal value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, principal, value, remainingPrincipal, remainingInterest, status, financedAgreementId); }
    public CardObligation withRemainingPrincipal(BigDecimal value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, principal, interest, value, remainingInterest, status, financedAgreementId); }
    public CardObligation withRemainingInterest(BigDecimal value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, value, status, financedAgreementId); }
    public CardObligation withStatus(String value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, value, financedAgreementId); }
    public CardObligation withFinancedAgreementId(UUID value) { return new CardObligation(id, customerId, brand, sourceType, sourceId, sequence, reference, nominalDue, effectiveDue, closingAt, principal, interest, remainingPrincipal, remainingInterest, status, value); }
}
