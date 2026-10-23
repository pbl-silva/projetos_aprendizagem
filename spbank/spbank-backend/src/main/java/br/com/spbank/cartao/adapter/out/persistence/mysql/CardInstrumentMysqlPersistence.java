package br.com.spbank.cartao.adapter.out.persistence.mysql;

import br.com.spbank.cartao.adapter.out.persistence.mysql.data.*;
import br.com.spbank.cartao.adapter.out.persistence.mysql.repository.*;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.out.CardInstrumentPersistence;
import java.util.*;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public final class CardInstrumentMysqlPersistence implements CardInstrumentPersistence {
    private final CardInstrumentJpaRepository cards;
    private final CardBlockAuditJpaRepository audit;
    public CardInstrumentMysqlPersistence(CardInstrumentJpaRepository cards, CardBlockAuditJpaRepository audit) {
        this.cards = cards; this.audit = audit;
    }
    @Override public List<CardInstrument> cards(UUID customerId) {
        return cards.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(value -> java.util.Objects.requireNonNull(value).toDomain()).toList();
    }
    @Override public Optional<CardInstrument> find(UUID customerId, UUID cardId) {
        return cards.findByIdAndCustomerId(cardId, customerId).map(value -> java.util.Objects.requireNonNull(value).toDomain());
    }
    @Override public Optional<CardInstrument> successor(UUID previousCardId) {
        return cards.findByPreviousCardId(previousCardId).map(value -> java.util.Objects.requireNonNull(value).toDomain());
    }
    @Override public Optional<UUID> ownerId(UUID cardId) {
        return cards.ownerId(cardId);
    }
    @Override public Optional<CardInstrument> findForUpdate(UUID customerId, UUID cardId) {
        return cards.findForUpdate(customerId, cardId).map(value -> java.util.Objects.requireNonNull(value).toDomain());
    }
    @Override public List<UUID> findLifecycleDueIds(LocalDate today, LocalDate through, int limit) {
        return cards.findLifecycleDueIds(today, through, CardState.ACTIVE, CardState.PENDING_ACTIVATION,
            PageRequest.of(0, limit));
    }
    @Override public void save(CardInstrument card) { cards.saveAndFlush(new CardInstrumentData(card)); }
    @Override public void audit(CardBlockAudit value) { audit.save(new CardBlockAuditData(value)); }
}
