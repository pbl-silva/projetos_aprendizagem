package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardAgreement(UUID id, UUID customerId, CardBrand brand, UUID proposalId, UUID previousAgreementId, BigDecimal principal, BigDecimal interest, BigDecimal rate, int installments, CustomerPlan plan, String status, Instant createdAt) implements CardFinanceRecord {
    public CardAgreement withProposalId(UUID value) { return new CardAgreement(id, customerId, brand, value, previousAgreementId, principal, interest, rate, installments, plan, status, createdAt); }
    public CardAgreement withPreviousAgreementId(UUID value) { return new CardAgreement(id, customerId, brand, proposalId, value, principal, interest, rate, installments, plan, status, createdAt); }
    public CardAgreement withPrincipal(BigDecimal value) { return new CardAgreement(id, customerId, brand, proposalId, previousAgreementId, value, interest, rate, installments, plan, status, createdAt); }
    public CardAgreement withInterest(BigDecimal value) { return new CardAgreement(id, customerId, brand, proposalId, previousAgreementId, principal, value, rate, installments, plan, status, createdAt); }
    public CardAgreement withRate(BigDecimal value) { return new CardAgreement(id, customerId, brand, proposalId, previousAgreementId, principal, interest, value, installments, plan, status, createdAt); }
    public CardAgreement withInstallments(int value) { return new CardAgreement(id, customerId, brand, proposalId, previousAgreementId, principal, interest, rate, value, plan, status, createdAt); }
    public CardAgreement withPlan(CustomerPlan value) { return new CardAgreement(id, customerId, brand, proposalId, previousAgreementId, principal, interest, rate, installments, value, status, createdAt); }
    public CardAgreement withStatus(String value) { return new CardAgreement(id, customerId, brand, proposalId, previousAgreementId, principal, interest, rate, installments, plan, value, createdAt); }
}
