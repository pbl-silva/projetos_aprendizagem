package br.com.spbank.mercado.application.usecase;

import br.com.spbank.mercado.application.model.MarketRatesSnapshot;
import br.com.spbank.mercado.application.port.out.MarketRatesProvider;
import br.com.spbank.mercado.configuration.MarketRatesProperties;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketRatesService {
    private static final Logger LOG = LoggerFactory.getLogger(MarketRatesService.class);
    private static final String SOURCE = "BrasilAPI";
    private final MarketRatesProvider provider;
    private final MarketRatesProperties properties;
    private final Clock clock;
    private CachedSnapshot cached;

    public MarketRatesService(MarketRatesProvider provider, MarketRatesProperties properties, Clock clock) {
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized MarketRatesSnapshot current() {
        var now = clock.instant();
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.snapshot();
        }
        try {
            var snapshot = new MarketRatesSnapshot(
                    MarketRatesSnapshot.Status.AVAILABLE,
                    provider.fetch(),
                    SOURCE,
                    now,
                    "Taxas anuais de referência. Não alteram os rendimentos simulados do SPBank."
            );
            cached = new CachedSnapshot(snapshot, now.plus(properties.cacheTtl()));
            return snapshot;
        } catch (RuntimeException failure) {
            LOG.warn("Falha ao consultar taxas de mercado; usando resposta segura. exceptionType={}",
                    failure.getClass().getSimpleName());
            if (cached != null) {
                var previous = cached.snapshot();
                var stale = new MarketRatesSnapshot(
                        MarketRatesSnapshot.Status.STALE,
                        previous.rates(),
                        previous.source(),
                        previous.consultedAt(),
                        "Última referência disponível. A fonte externa está temporariamente indisponível."
                );
                cached = new CachedSnapshot(stale, now.plus(properties.failureCacheTtl()));
                return stale;
            }
            var unavailable = new MarketRatesSnapshot(
                    MarketRatesSnapshot.Status.UNAVAILABLE,
                    java.util.List.of(),
                    SOURCE,
                    now,
                    "Referências de Selic e CDI temporariamente indisponíveis."
            );
            cached = new CachedSnapshot(unavailable, now.plus(properties.failureCacheTtl()));
            return unavailable;
        }
    }

    private record CachedSnapshot(MarketRatesSnapshot snapshot, Instant expiresAt) {}
}
