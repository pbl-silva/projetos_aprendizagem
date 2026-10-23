package br.com.spbank.cartao.application.model.finance;
import br.com.spbank.cartao.application.model.CardBrand;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record CardPayment(UUID id, UUID customerId, CardBrand brand, String purpose, UUID proposalId, String method, BigDecimal amount, BigDecimal appliedAmount, BigDecimal creditedAmount, BigDecimal discount, String status, UUID accountId, UUID administratorId, String reason, String code, Instant createdAt, Instant paidAt) implements CardFinanceRecord {
    public CardPayment withPurpose(String value) { return new CardPayment(id, customerId, brand, value, proposalId, method, amount, appliedAmount, creditedAmount, discount, status, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withProposalId(UUID value) { return new CardPayment(id, customerId, brand, purpose, value, method, amount, appliedAmount, creditedAmount, discount, status, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withMethod(String value) { return new CardPayment(id, customerId, brand, purpose, proposalId, value, amount, appliedAmount, creditedAmount, discount, status, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withAmount(BigDecimal value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, value, appliedAmount, creditedAmount, discount, status, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withAppliedAmount(BigDecimal value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, value, creditedAmount, discount, status, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withCreditedAmount(BigDecimal value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, value, discount, status, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withDiscount(BigDecimal value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, value, status, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withStatus(String value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, discount, value, accountId, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withAccountId(UUID value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, discount, status, value, administratorId, reason, code, createdAt, paidAt); }
    public CardPayment withAdministratorId(UUID value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, discount, status, accountId, value, reason, code, createdAt, paidAt); }
    public CardPayment withReason(String value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, discount, status, accountId, administratorId, value, code, createdAt, paidAt); }
    public CardPayment withCode(String value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, discount, status, accountId, administratorId, reason, value, createdAt, paidAt); }
    public CardPayment withPaidAt(Instant value) { return new CardPayment(id, customerId, brand, purpose, proposalId, method, amount, appliedAmount, creditedAmount, discount, status, accountId, administratorId, reason, code, createdAt, value); }
}
