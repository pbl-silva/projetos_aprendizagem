package br.com.spbank.cartao.application.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditRequest(UUID id, UUID customerId, BigDecimal requestedTotal,
        String reason, CreditRequestStatus status, BigDecimal offeredTotal,
        UUID administratorId, String decisionReason, Instant createdAt,
        Instant decidedAt, Instant expiresAt, Instant closedAt) {
    public CreditRequestStatus statusAt(Instant now) {
        return status == CreditRequestStatus.OFFERED && !now.isBefore(expiresAt)
            ? CreditRequestStatus.EXPIRED : status;
    }
    public boolean isOpenAt(Instant now) {
        var current = statusAt(now);
        return current == CreditRequestStatus.REQUESTED || current == CreditRequestStatus.OFFERED;
    }
    public CreditRequest close(CreditRequestStatus next, Instant now) {
        return new CreditRequest(id, customerId, requestedTotal, reason, next,
            offeredTotal, administratorId, decisionReason, createdAt, decidedAt, expiresAt, now);
    }
    public CreditRequest expireIfDue(Instant now) {
        return statusAt(now) == CreditRequestStatus.EXPIRED && status != CreditRequestStatus.EXPIRED
            ? close(CreditRequestStatus.EXPIRED, expiresAt) : this;
    }
}
