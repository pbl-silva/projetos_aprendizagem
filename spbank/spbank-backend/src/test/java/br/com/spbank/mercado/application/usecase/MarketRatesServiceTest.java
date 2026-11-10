package br.com.spbank.mercado.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.spbank.mercado.application.model.MarketRate;
import br.com.spbank.mercado.application.model.MarketRatesSnapshot;
import br.com.spbank.mercado.application.port.out.MarketRatesProvider;
import br.com.spbank.mercado.configuration.MarketRatesProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MarketRatesServiceTest {
    private final MutableClock clock = new MutableClock();
    private final MarketRatesProperties properties = new MarketRatesProperties(
            URI.create("https://example.test/rates"), Duration.ofSeconds(1), Duration.ofSeconds(1),
            Duration.ofMinutes(30), Duration.ofMinutes(1));

    @Test
    void cachesAvailableRatesWithinConfiguredTtl() {
        var calls = new AtomicInteger();
        MarketRatesProvider provider = () -> {
            calls.incrementAndGet();
            return rates();
        };
        var service = new MarketRatesService(provider, properties, clock);

        assertThat(service.current().status()).isEqualTo(MarketRatesSnapshot.Status.AVAILABLE);
        clock.advance(Duration.ofMinutes(29));
        assertThat(service.current().rates()).containsExactlyElementsOf(rates());
        assertThat(calls).hasValue(1);
    }

    @Test
    void servesLastSnapshotAsStaleWhenRefreshFails() {
        var fail = new boolean[] {false};
        MarketRatesProvider provider = () -> {
            if (fail[0]) throw new IllegalStateException("offline");
            return rates();
        };
        var service = new MarketRatesService(provider, properties, clock);
        var first = service.current();

        fail[0] = true;
        clock.advance(Duration.ofMinutes(31));
        var stale = service.current();

        assertThat(stale.status()).isEqualTo(MarketRatesSnapshot.Status.STALE);
        assertThat(stale.rates()).isEqualTo(first.rates());
        assertThat(stale.consultedAt()).isEqualTo(first.consultedAt());
    }

    @Test
    void returnsSafeUnavailableSnapshotWhenFirstRequestFails() {
        var calls = new AtomicInteger();
        var service = new MarketRatesService(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("offline");
        }, properties, clock);

        var snapshot = service.current();
        var cachedFailure = service.current();

        assertThat(snapshot.status()).isEqualTo(MarketRatesSnapshot.Status.UNAVAILABLE);
        assertThat(snapshot.rates()).isEmpty();
        assertThat(snapshot.source()).isEqualTo("BrasilAPI");
        assertThat(cachedFailure).isEqualTo(snapshot);
        assertThat(calls).hasValue(1);
    }

    private static List<MarketRate> rates() {
        return List.of(new MarketRate("SELIC", new BigDecimal("13.75")), new MarketRate("CDI", new BigDecimal("13.65")));
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-10T12:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
