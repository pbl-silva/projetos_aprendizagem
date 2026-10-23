package br.com.spbank.cartao.adapter.in.scheduler;

import br.com.spbank.cartao.application.port.in.ProcessCardLifecycleUseCase;
import br.com.spbank.cartao.application.port.out.CardInstrumentPersistence;
import java.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class CardLifecycleProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CardLifecycleProcessor.class);
    private final CardInstrumentPersistence cards;
    private final ProcessCardLifecycleUseCase lifecycle;
    private final Clock clock;
    private final ZoneId zone;

    public CardLifecycleProcessor(CardInstrumentPersistence cards, ProcessCardLifecycleUseCase lifecycle,
            Clock clock, @Value("${spbank.zone}") String zone) {
        this.cards = cards; this.lifecycle = lifecycle; this.clock = clock; this.zone = ZoneId.of(zone);
    }

    @Scheduled(fixedDelayString = "${spbank.card.scheduler-delay:60000}")
    public void processDueBatch() {
        var today = LocalDate.now(clock.withZone(zone));
        for (var id : cards.findLifecycleDueIds(today, today.plusDays(30), 100)) {
            try { lifecycle.process(id); }
            catch (RuntimeException ex) {
                LOG.error("Falha no ciclo de cartão cardId={} exceptionType={}", id, ex.getClass().getSimpleName());
            }
        }
    }
}
