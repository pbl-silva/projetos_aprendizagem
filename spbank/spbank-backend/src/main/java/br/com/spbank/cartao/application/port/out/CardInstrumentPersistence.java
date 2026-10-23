package br.com.spbank.cartao.application.port.out;

import br.com.spbank.cartao.application.model.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface CardInstrumentPersistence {
    List<CardInstrument> cards(UUID customerId);
    Optional<CardInstrument> find(UUID customerId, UUID cardId);
    Optional<UUID> ownerId(UUID cardId);
    Optional<CardInstrument> findForUpdate(UUID customerId, UUID cardId);
    List<UUID> findLifecycleDueIds(LocalDate today, LocalDate renewalThrough, int limit);
    Optional<CardInstrument> successor(UUID previousCardId);
    void save(CardInstrument card);
    void audit(CardBlockAudit audit);
}
