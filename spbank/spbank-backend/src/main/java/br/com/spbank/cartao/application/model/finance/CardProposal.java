package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardProposal(UUID id, UUID customerId, CardBrand brand, String coveredIds, UUID previousAgreementId, BigDecimal debt, BigDecimal rate, BigDecimal interest, BigDecimal entry, BigDecimal total, int installments, CustomerPlan plan, LocalDate validOn, String status, Instant createdAt) implements CardFinanceRecord {
    public CardProposal withCoveredIds(String value) { return new CardProposal(id, customerId, brand, value, previousAgreementId, debt, rate, interest, entry, total, installments, plan, validOn, status, createdAt); }
    public CardProposal withPreviousAgreementId(UUID value) { return new CardProposal(id, customerId, brand, coveredIds, value, debt, rate, interest, entry, total, installments, plan, validOn, status, createdAt); }
    public CardProposal withDebt(BigDecimal value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, value, rate, interest, entry, total, installments, plan, validOn, status, createdAt); }
    public CardProposal withRate(BigDecimal value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, value, interest, entry, total, installments, plan, validOn, status, createdAt); }
    public CardProposal withInterest(BigDecimal value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, value, entry, total, installments, plan, validOn, status, createdAt); }
    public CardProposal withEntry(BigDecimal value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, interest, value, total, installments, plan, validOn, status, createdAt); }
    public CardProposal withTotal(BigDecimal value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, interest, entry, value, installments, plan, validOn, status, createdAt); }
    public CardProposal withInstallments(int value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, interest, entry, total, value, plan, validOn, status, createdAt); }
    public CardProposal withPlan(CustomerPlan value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, interest, entry, total, installments, value, validOn, status, createdAt); }
    public CardProposal withValidOn(LocalDate value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, interest, entry, total, installments, plan, value, status, createdAt); }
    public CardProposal withStatus(String value) { return new CardProposal(id, customerId, brand, coveredIds, previousAgreementId, debt, rate, interest, entry, total, installments, plan, validOn, value, createdAt); }
}
