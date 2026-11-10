package br.com.spbank.endereco.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.spbank.endereco.application.model.PostalAddress;
import br.com.spbank.endereco.application.model.PostalCodeLookup;
import br.com.spbank.endereco.application.port.out.PostalCodeProvider;
import br.com.spbank.endereco.configuration.PostalCodeProperties;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PostalCodeLookupServiceTest {
    private final MutableClock clock = new MutableClock();
    private final PostalCodeProperties properties = new PostalCodeProperties(
            URI.create("https://example.test/ws/"), Duration.ofSeconds(1), Duration.ofSeconds(1),
            Duration.ofHours(24), Duration.ofMinutes(5), Duration.ofMinutes(1), 100);

    @Test
    void normalizesAndCachesAnAvailableAddress() {
        var calls = new AtomicInteger();
        PostalCodeProvider provider = postalCode -> {
            calls.incrementAndGet();
            return Optional.of(address(postalCode));
        };
        var service = new PostalCodeLookupService(provider, properties, clock);

        var first = service.find("01001-000");
        var cached = service.find("01001000");

        assertThat(first.status()).isEqualTo(PostalCodeLookup.Status.AVAILABLE);
        assertThat(first.street()).isEqualTo("Praça da Sé");
        assertThat(cached).isEqualTo(first);
        assertThat(calls).hasValue(1);
    }

    @Test
    void cachesNotFoundWithoutInventingAddressFields() {
        var calls = new AtomicInteger();
        var service = new PostalCodeLookupService(postalCode -> {
            calls.incrementAndGet();
            return Optional.empty();
        }, properties, clock);

        var result = service.find("99999999");
        service.find("99999999");

        assertThat(result.status()).isEqualTo(PostalCodeLookup.Status.NOT_FOUND);
        assertThat(result.street()).isEmpty();
        assertThat(calls).hasValue(1);
    }

    @Test
    void providerFailureDoesNotPreventManualAddressEntryAndIsBrieflyCached() {
        var calls = new AtomicInteger();
        var service = new PostalCodeLookupService(postalCode -> {
            calls.incrementAndGet();
            throw new IllegalStateException("offline");
        }, properties, clock);

        var result = service.find("01001000");
        service.find("01001000");

        assertThat(result.status()).isEqualTo(PostalCodeLookup.Status.UNAVAILABLE);
        assertThat(result.notice()).contains("manualmente");
        assertThat(calls).hasValue(1);
    }

    private static PostalAddress address(String postalCode) {
        return new PostalAddress(postalCode, "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP");
    }

    private static final class MutableClock extends Clock {
        private final Instant now = Instant.parse("2026-11-10T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
