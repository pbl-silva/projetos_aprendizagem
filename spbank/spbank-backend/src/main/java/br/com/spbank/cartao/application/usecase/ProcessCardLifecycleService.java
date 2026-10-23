package br.com.spbank.cartao.application.usecase;

import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.cartao.application.model.CardState;
import br.com.spbank.cartao.application.port.in.ProcessCardLifecycleUseCase;
import br.com.spbank.cartao.application.port.out.CardCredentialVault;
import br.com.spbank.cartao.application.port.out.CardInstrumentPersistence;
import br.com.spbank.cartao.application.service.CardInstrumentFactory;
import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessCardLifecycleService implements ProcessCardLifecycleUseCase {
    private final CardInstrumentPersistence cards;
    private final AuthenticationPersistence customers;
    private final CardCredentialVault vault;
    private final Clock clock;
    private final ZoneId zone;

    public ProcessCardLifecycleService(CardInstrumentPersistence cards, AuthenticationPersistence customers,
            CardCredentialVault vault, Clock clock, @Value("${spbank.zone}") String zone) {
        this.cards = cards; this.customers = customers; this.vault = vault;
        this.clock = clock; this.zone = ZoneId.of(zone);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID cardId) {
        // Scalar lookup avoids caching an instrument before waiting for the customer lock.
        var customerId = cards.ownerId(cardId).orElse(null);
        if (customerId == null) return;
        // Same lock order as client mutations: customer first, then instrument.
        var customer = customers.findCustomerByIdForUpdate(customerId).orElse(null);
        if (customer == null) return;
        var card = cards.findForUpdate(customer.id(), cardId).orElse(null);
        if (card == null) return;
        var today = LocalDate.now(clock.withZone(zone));
        if (card.state() != CardState.ACTIVE && card.state() != CardState.PENDING_ACTIVATION) return;
        if (today.isAfter(card.expiresOn())) {
            cards.save(card.withState(CardState.EXPIRED));
            return;
        }
        if (card.state() != CardState.ACTIVE || card.expiresOn().isAfter(today.plusDays(30))) return;
        if (cards.successor(cardId).isPresent()) return;
        // Extends the existing line, including a zero allocation; it grants no new credit.
        cards.save(CardInstrumentFactory.issue(customer.id(), customer.fullName(), card.brand(),
            card, vault.issue(card.brand()), today, clock.instant()));
    }
}
