package br.com.spbank.endereco.application.usecase;

import br.com.spbank.endereco.application.model.PostalAddress;
import br.com.spbank.endereco.application.model.PostalCodeLookup;
import br.com.spbank.endereco.application.port.out.PostalCodeProvider;
import br.com.spbank.endereco.configuration.PostalCodeProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PostalCodeLookupService {
    private static final Logger LOG = LoggerFactory.getLogger(PostalCodeLookupService.class);
    private static final String SOURCE = "ViaCEP";
    private final PostalCodeProvider provider;
    private final PostalCodeProperties properties;
    private final Clock clock;
    private final Map<String, CachedLookup> cache;

    public PostalCodeLookupService(PostalCodeProvider provider, PostalCodeProperties properties, Clock clock) {
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
        var maximumSize = properties.maxCacheEntries();
        this.cache = new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedLookup> eldest) {
                return size() > maximumSize;
            }
        };
    }

    public PostalCodeLookup find(String value) {
        var postalCode = normalize(value);
        var now = clock.instant();
        synchronized (cache) {
            var cached = cache.get(postalCode);
            if (cached != null && now.isBefore(cached.expiresAt())) return cached.lookup();
            if (cached != null) cache.remove(postalCode);
        }

        PostalCodeLookup lookup;
        Duration ttl;
        try {
            var address = provider.find(postalCode);
            if (address.isPresent()) {
                lookup = available(address.orElseThrow(), now);
                ttl = properties.successCacheTtl();
            } else {
                lookup = empty(PostalCodeLookup.Status.NOT_FOUND, postalCode, now,
                        "CEP não encontrado. Confira os números ou preencha o endereço manualmente.");
                ttl = properties.notFoundCacheTtl();
            }
        } catch (RuntimeException failure) {
            LOG.warn("Falha ao consultar CEP; permitindo preenchimento manual. exceptionType={}",
                    failure.getClass().getSimpleName());
            lookup = empty(PostalCodeLookup.Status.UNAVAILABLE, postalCode, now,
                    "Consulta de CEP temporariamente indisponível. Preencha o endereço manualmente.");
            ttl = properties.failureCacheTtl();
        }
        synchronized (cache) {
            cache.put(postalCode, new CachedLookup(lookup, now.plus(ttl)));
        }
        return lookup;
    }

    private static PostalCodeLookup available(PostalAddress address, Instant now) {
        return new PostalCodeLookup(PostalCodeLookup.Status.AVAILABLE, address.postalCode(), address.street(),
                address.complement(), address.district(), address.city(), address.state(), SOURCE, now,
                "Endereço localizado. Confira os dados e informe o número.");
    }

    private static PostalCodeLookup empty(PostalCodeLookup.Status status, String postalCode, Instant now, String notice) {
        return new PostalCodeLookup(status, postalCode, "", "", "", "", "", SOURCE, now, notice);
    }

    private static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("Postal code is required");
        var normalized = value.replaceAll("\\D", "");
        if (!normalized.matches("\\d{8}")) throw new IllegalArgumentException("Postal code must have eight digits");
        return normalized;
    }

    private record CachedLookup(PostalCodeLookup lookup, Instant expiresAt) {}
}
