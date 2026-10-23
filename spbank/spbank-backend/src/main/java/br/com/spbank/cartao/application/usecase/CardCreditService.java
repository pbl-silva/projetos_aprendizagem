package br.com.spbank.cartao.application.usecase;

import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.in.CardCreditUseCase;
import br.com.spbank.cartao.application.port.out.CardCreditPersistence;
import br.com.spbank.cartao.application.service.CardCreditPolicy;
import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.shared.application.exception.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardCreditService implements CardCreditUseCase {
    private final CardCreditPersistence credit;
    private final AuthenticationPersistence customers;
    private final AdministrationPersistence administrators;
    private final Clock clock;
    public CardCreditService(CardCreditPersistence credit, AuthenticationPersistence customers,
            AdministrationPersistence administrators, Clock clock) {
        this.credit = credit; this.customers = customers;
        this.administrators = administrators; this.clock = clock;
    }
    @Override @Transactional(readOnly = true)
    public CreditOverview overview(UUID customerId) {
        var customer = customers.findCustomerById(customerId).filter(c -> c.active())
            .orElseThrow(UnauthorizedException::new);
        return new CreditOverview(customer.plan(), CardCreditPolicy.ceiling(customer.plan()),
            credit.approvedTotal(customerId), credit.requests(customerId).stream()
                .map(r -> r.expireIfDue(clock.instant())).toList());
    }
    @Override @Transactional(readOnly = true)
    public List<CreditRequest> pending(UUID administratorId) {
        requireAdministrator(administratorId);
        return credit.pendingRequests().stream()
            .filter(r -> r.statusAt(clock.instant()) == CreditRequestStatus.REQUESTED).toList();
    }
    @Override
    public CreditResult request(UUID customerId, UUID key, BigDecimal total, String reason) {
        var customer = lockCustomer(customerId);
        total = amount(total); reason = reason(reason);
        String hash = fingerprint("REQUEST", customerId, "", total.toPlainString(), reason);
        var replay = replay(customerId, key, hash);
        if (replay.isPresent()) return replay.get();
        requireIncrease(total, credit.approvedTotal(customerId), customer);
        for (var previous : credit.requests(customerId)) {
            if (previous.isOpenAt(clock.instant())) throw error("OPEN_REQUEST", "open-request");
            if (previous.status() == CreditRequestStatus.OFFERED)
                credit.saveRequest(previous.expireIfDue(clock.instant()));
        }
        var request = new CreditRequest(UUID.randomUUID(), customerId, total, reason,
            CreditRequestStatus.REQUESTED, null, null, null, clock.instant(), null, null, null);
        return record(customerId, key, hash, customerId, "REQUEST", request);
    }
    @Override
    public CreditResult decide(UUID administratorId, UUID customerId, UUID requestId,
            UUID key, CreditDecision decision, BigDecimal total, String reason) {
        requireAdministrator(administratorId);
        var customer = lockCustomer(customerId);
        if (decision == null) throw error("DECISION_REQUIRED", "decision");
        reason = reason(reason);
        if (decision == CreditDecision.OFFER) total = amount(total);
        else if (total != null) throw error("REJECTION_TOTAL", "rejection-total");
        String hash = fingerprint("DECIDE", administratorId, requestId.toString(),
            decision.name(), total == null ? "" : total.toPlainString(), reason);
        var replay = replay(customerId, key, hash);
        if (replay.isPresent()) return replay.get();
        var request = requireRequest(customerId, requestId);
        if (request.status() != CreditRequestStatus.REQUESTED) throw error("STATE", "state");
        if (decision == CreditDecision.OFFER) {
            requireIncrease(total, credit.approvedTotal(customerId), customer);
            if (java.util.Objects.requireNonNull(total).compareTo(request.requestedTotal()) > 0) throw error("OFFER_TOTAL", "offer-total");
        }
        var now = clock.instant();
        var next = new CreditRequest(request.id(), customerId, request.requestedTotal(),
            request.reason(), decision == CreditDecision.OFFER ? CreditRequestStatus.OFFERED
                : CreditRequestStatus.REJECTED, total, administratorId, reason, request.createdAt(),
            now, decision == CreditDecision.OFFER ? now.plus(Duration.ofDays(7)) : null,
            decision == CreditDecision.REJECT ? now : null);
        return record(customerId, key, hash, administratorId, "DECIDE", next);
    }
    @Override
    public CreditResult accept(UUID customerId, UUID requestId, UUID key, String password) {
        var customer = lockCustomer(customerId);
        boolean valid = customers.findCredentialByCustomerId(customerId)
            .filter(c -> c.active()).filter(c -> PasswordHasher.matches(password, c.passwordHash()))
            .isPresent();
        if (!valid) throw error("PASSWORD", "password");
        String hash = fingerprint("ACCEPT", customerId, requestId.toString());
        var replay = replay(customerId, key, hash);
        if (replay.isPresent()) return replay.get();
        var request = requireRequest(customerId, requestId);
        if (request.statusAt(clock.instant()) == CreditRequestStatus.EXPIRED)
            throw error("EXPIRED", "expired");
        if (request.status() != CreditRequestStatus.OFFERED) throw error("STATE", "state");
        requireIncrease(request.offeredTotal(), credit.approvedTotal(customerId), customer);
        // Aceitar aprova o total, mas não emite instrumentos nem aloca limites por bandeira.
        var initial = CustomerCardCredit.initial(customerId);
        var current = new CustomerCardCredit(customerId, credit.approvedTotal(customerId), initial.lines());
        var approved = current.acceptApprovedTotal(request.offeredTotal(), customer.plan());
        credit.saveApprovedTotal(customerId, approved.approvedTotal());
        return record(customerId, key, hash, customerId, "ACCEPT",
            request.close(CreditRequestStatus.ACCEPTED, clock.instant()));
    }
    @Override
    public CreditResult cancel(UUID customerId, UUID requestId, UUID key) {
        lockCustomer(customerId);
        String hash = fingerprint("CANCEL", customerId, requestId.toString());
        var replay = replay(customerId, key, hash);
        if (replay.isPresent()) return replay.get();
        var request = requireRequest(customerId, requestId);
        if (!request.isOpenAt(clock.instant())) throw error("STATE", "state");
        return record(customerId, key, hash, customerId, "CANCEL",
            request.close(CreditRequestStatus.CANCELLED, clock.instant()));
    }
    private CreditResult record(UUID customerId, UUID key, String hash, UUID actor,
            String action, CreditRequest request) {
        credit.saveRequest(request);
        var result = new CreditResult(UUID.randomUUID(), actor, action, request,
            credit.approvedTotal(customerId), clock.instant());
        credit.saveOperation(new CreditOperation(customerId, key, hash, result));
        return result;
    }
    private Optional<CreditResult> replay(UUID customerId, UUID key, String hash) {
        Objects.requireNonNull(key, "idempotencyKey");
        return credit.findOperation(customerId, key).map(op -> {
            if (!op.fingerprint().equals(hash)) throw new IdempotencyConflictException();
            return op.result();
        });
    }
    private Customer lockCustomer(UUID id) {
        return customers.findCustomerByIdForUpdate(id).filter(c -> c.active())
            .orElseThrow(UnauthorizedException::new);
    }
    private CreditRequest requireRequest(UUID customerId, UUID requestId) {
        return credit.findRequest(customerId, requestId)
            .orElseThrow(() -> new NotFoundException("CARD_CREDIT_REQUEST_NOT_FOUND"));
    }
    private void requireAdministrator(UUID id) {
        administrators.findCredentialById(id).filter(a -> a.active())
            .orElseThrow(UnauthorizedException::new);
    }
    private static BigDecimal amount(BigDecimal total) {
        if (total == null || total.signum() <= 0 || total.scale() < 0 || total.scale() > 2
                || total.precision() - total.scale() > 17) throw error("AMOUNT", "amount");
        return total.setScale(2);
    }
    private static String reason(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 240)
            throw error("REASON", "reason");
        return value.trim();
    }
    private static void requireIncrease(BigDecimal total, BigDecimal current, Customer customer) {
        if (total.compareTo(current) <= 0 || total.compareTo(CardCreditPolicy.ceiling(customer.plan())) > 0)
            throw error("TOTAL", "total");
    }
    private static String fingerprint(String action, UUID actor, String... values) {
        // Nunca incluir senha no fingerprint ou no histórico.
        return PasswordHasher.tokenHash(action + "|" + actor + "|" + String.join("|", values));
    }
    private static BusinessException error(String code, String key) {
        return new BusinessException("CARD_CREDIT_" + code, "card.request." + key);
    }
}
