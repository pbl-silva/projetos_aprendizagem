package br.com.spbank.cartao.application.usecase;

import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.cartao.application.exception.CardPinException;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.service.CardInstrumentFactory;
import br.com.spbank.cartao.application.port.in.CardInstrumentUseCase;
import br.com.spbank.cartao.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = CardPinException.class)
public class CardInstrumentService implements CardInstrumentUseCase {
    private final CardInstrumentPersistence cards;
    private final CardCreditAllocationPersistence allocations;
    private final AuthenticationPersistence customers;
    private final AdministrationPersistence administrators;
    private final CardCredentialVault vault;
    private final Clock clock;
    private final ZoneId zone;

    public CardInstrumentService(CardInstrumentPersistence cards, CardCreditAllocationPersistence allocations,
            AuthenticationPersistence customers, AdministrationPersistence administrators,
            CardCredentialVault vault, Clock clock, @Value("${spbank.zone}") String zone) {
        this.cards = cards; this.allocations = allocations; this.customers = customers;
        this.administrators = administrators; this.vault = vault; this.clock = clock; this.zone = ZoneId.of(zone);
    }

    @Override @Transactional(readOnly = true)
    public List<CardSummary> cards(UUID customerId) {
        customers.findCustomerById(customerId).filter((Customer value) -> java.util.Objects.requireNonNull(value).active()).orElseThrow(UnauthorizedException::new);
        return cards.cards(customerId).stream().map(this::summary).toList();
    }

    @Override public CardSummary issue(UUID customerId, CardBrand brand) {
        var customer = lockCustomer(customerId);
        if (brand == null) throw error("BRAND", "brand");
        if (cards.cards(customerId).stream().anyMatch(card -> card.brand() == brand))
            throw error("EXISTS", "exists");
        return summary(create(customer, brand, null));
    }

    private CardInstrument create(Customer customer, CardBrand brand, CardInstrument previous) {
        if (line(customer.id(), brand).allocated().signum() <= 0) throw error("NO_ALLOCATION", "no-allocation");
        var credentials = vault.issue(brand);
        var card = CardInstrumentFactory.issue(customer.id(), customer.fullName(), brand,
            previous, credentials, today(), clock.instant());
        cards.save(card);
        return card;
    }

    @Override public CardSummary activate(UUID customerId, UUID cardId, String password, String pin, String confirmation) {
        lockCustomer(customerId); requirePassword(customerId, password); validateNewPin(customerId, pin, confirmation);
        var card = requireCard(customerId, cardId);
        if (card.stateAt(today()) != CardState.PENDING_ACTIVATION) throw error("STATE", "state");
        var previousId = card.previousCardId();
        var visited = new java.util.HashSet<UUID>();
        visited.add(card.id());
        while (previousId != null) {
            if (!visited.add(previousId)) throw error("STATE", "state");
            var previous = cards.findForUpdate(customerId, previousId)
                .orElseThrow(() -> new NotFoundException("CARD_NOT_FOUND"));
            if (previous.brand() != card.brand()) throw error("STATE", "state");
            // Preserve restrictions applied after renewal was issued; no activation may bypass them.
            boolean livePredecessor = previous.state() == CardState.ACTIVE || previous.state() == CardState.EXPIRED;
            if (livePredecessor || previous.id().equals(card.previousCardId())) {
                card = card.withBlocks(card.voluntaryBlocked() || previous.voluntaryBlocked(),
                    card.administrativeBlocked() || previous.administrativeBlocked(),
                    previous.voluntaryBlocked() ? previous.voluntaryReason() : card.voluntaryReason(),
                    previous.administrativeBlocked() ? previous.administrativeReason() : card.administrativeReason());
            }
            if (livePredecessor) {
                // Flush the old ACTIVE slot before occupying it with the new instrument.
                cards.save(previous.withState(CardState.REPLACED));
                break;
            }
            if (previous.state() == CardState.REPLACED) break;
            previousId = previous.previousCardId();
        }
        var updated = card.withPin(PasswordHasher.hash(pin), 0, false).withState(CardState.ACTIVE);
        cards.save(updated);
        return summary(updated);
    }

    @Override public CardSummary changePin(UUID customerId, UUID cardId, String currentPin, String newPin, String confirmation) {
        lockCustomer(customerId); validateNewPin(customerId, newPin, confirmation);
        var card = requireActive(customerId, cardId);
        verifyPin(card, currentPin);
        var updated = card.withPin(PasswordHasher.hash(newPin), 0, false);
        cards.save(updated); return summary(updated);
    }

    @Override public CardSummary recoverPin(UUID customerId, UUID cardId, String password, String newPin, String confirmation) {
        lockCustomer(customerId); requirePassword(customerId, password); validateNewPin(customerId, newPin, confirmation);
        var card = requireActive(customerId, cardId);
        var updated = card.withPin(PasswordHasher.hash(newPin), 0, false);
        cards.save(updated); return summary(updated);
    }

    @Override public CardDetails details(UUID customerId, UUID cardId, String pin) {
        lockCustomer(customerId);
        var card = requireActive(customerId, cardId);
        verifyPin(card, pin);
        var details = vault.reveal(card);
        if (card.pinFailures() != 0) cards.save(card.withPin(card.pinHash(), 0, false));
        return details;
    }

    private void verifyPin(CardInstrument card, String pin) {
        if (card.pinBlocked()) throw new CardPinException(true);
        if (pin == null || !pin.matches("[0-9]{4}") || !PasswordHasher.matches(pin, card.pinHash())) {
            int failures = Math.min(3, card.pinFailures() + 1);
            cards.save(card.withPin(card.pinHash(), failures, failures == 3));
            throw new CardPinException(failures == 3);
        }
    }

    @Override public CardSummary block(UUID customerId, UUID cardId, String reason) {
        lockCustomer(customerId); reason = reason(reason);
        var card = requireCard(customerId, cardId);
        requireLive(card);
        if (card.voluntaryBlocked()) return summary(card);
        var updated = card.withBlocks(true, card.administrativeBlocked(), reason, card.administrativeReason());
        cards.save(updated); audit(updated, customerId, "VOLUNTARY_BLOCK", reason); return summary(updated);
    }

    @Override public CardSummary cancel(UUID customerId, UUID cardId) {
        lockCustomer(customerId);
        var card = requireCard(customerId, cardId);
        if (card.state() == CardState.REPLACED) throw error("STATE", "state");
        var updated = card.withState(CardState.CANCELLED);
        cards.save(updated); return summary(updated);
    }

    @Override public CardSummary replace(UUID customerId, UUID cardId) {
        var customer = lockCustomer(customerId);
        var previous = requireCard(customerId, cardId);
        if (previous.state() != CardState.CANCELLED) throw error("STATE", "state");
        return cards.successor(cardId).map(this::summary)
            .orElseGet(() -> summary(create(customer, previous.brand(), previous)));
    }

    @Override @Transactional(readOnly = true)
    public List<CardSummary> administrativeCards(UUID administratorId, UUID customerId) {
        requireAdministrator(administratorId);
        return cards(customerId);
    }

    @Override public CardSummary administrativeBlock(UUID administratorId, UUID customerId, UUID cardId,
            String cause, boolean blocked, String reason) {
        requireAdministrator(administratorId); lockCustomer(customerId); reason = reason(reason);
        var card = requireCard(customerId, cardId);
        if (!"VOLUNTARY".equals(cause) && !"ADMINISTRATIVE".equals(cause)) throw error("CAUSE", "cause");
        if ("VOLUNTARY".equals(cause) && blocked) throw error("CAUSE", "cause");
        var updated = "VOLUNTARY".equals(cause)
            ? card.withBlocks(false, card.administrativeBlocked(), null, card.administrativeReason())
            : card.withBlocks(card.voluntaryBlocked(), blocked, card.voluntaryReason(), blocked ? reason : null);
        cards.save(updated); audit(updated, administratorId, cause + (blocked ? "_BLOCK" : "_UNBLOCK"), reason);
        return summary(updated);
    }

    private void audit(CardInstrument card, UUID actor, String action, String reason) {
        cards.audit(new CardBlockAudit(UUID.randomUUID(), card.id(), actor, action, reason, clock.instant()));
    }

    private CardSummary summary(CardInstrument card) {
        var line = line(card.customerId(), card.brand());
        return new CardSummary(card.id(), card.brand(), card.lastFour(), card.stateAt(today()), card.pinBlocked(),
            card.voluntaryBlocked(), card.administrativeBlocked(), card.voluntaryReason(), card.administrativeReason(),
            card.expiresOn(), line.allocated(), line.committed(), line.available(), line.excess());
    }

    private CardCreditLine line(UUID customerId, CardBrand brand) {
        return allocations.lines(customerId).stream().filter(line -> line.brand() == brand).findFirst()
            .orElse(new CardCreditLine(brand, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private Customer lockCustomer(UUID id) {
        return customers.findCustomerByIdForUpdate(id).filter((Customer value) -> java.util.Objects.requireNonNull(value).active()).orElseThrow(UnauthorizedException::new);
    }

    private CardInstrument requireCard(UUID customerId, UUID cardId) {
        return cards.findForUpdate(customerId, cardId).orElseThrow(() -> new NotFoundException("CARD_NOT_FOUND"));
    }

    private CardInstrument requireActive(UUID customerId, UUID cardId) {
        var card = requireCard(customerId, cardId);
        if (card.stateAt(today()) != CardState.ACTIVE) throw error("STATE", "state");
        return card;
    }

    private void requireLive(CardInstrument card) {
        var state = card.stateAt(today());
        if (state != CardState.ACTIVE && state != CardState.PENDING_ACTIVATION) throw error("STATE", "state");
    }

    private void requirePassword(UUID customerId, String password) {
        if (customers.findCredentialByCustomerId(customerId).filter(c -> c.active())
            .filter(c -> PasswordHasher.matches(password, c.passwordHash())).isEmpty())
            throw new BusinessException("CARD_CREDIT_PASSWORD", "card.request.password");
    }

    private void requireAdministrator(UUID id) {
        administrators.findCredentialById(id).filter(a -> a.active()).orElseThrow(UnauthorizedException::new);
    }

    private void validateNewPin(UUID customerId, String pin, String confirmation) {
        if (pin == null || !pin.matches("[0-9]{4}") || !pin.equals(confirmation)) throw error("PIN_FORMAT", "pin-format");
        if (customers.findCredentialByCustomerId(customerId)
            .filter(c -> PasswordHasher.matches(pin, c.passwordHash())).isPresent()) throw error("PIN_FORMAT", "pin-format");
    }

    private static String reason(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 240) throw error("REASON", "reason");
        return value.strip();
    }

    private LocalDate today() { return LocalDate.now(clock.withZone(zone)); }
    private static BusinessException error(String code, String key) {
        return new BusinessException("CARD_INSTRUMENT_" + code, "card.instrument." + key);
    }
}
