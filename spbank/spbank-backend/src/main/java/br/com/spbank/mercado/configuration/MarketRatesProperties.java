package br.com.spbank.mercado.configuration;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spbank.market")
public record MarketRatesProperties(
        URI ratesUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration cacheTtl,
        Duration failureCacheTtl
) {
    public MarketRatesProperties {
        Objects.requireNonNull(ratesUrl, "ratesUrl");
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(requestTimeout, "requestTimeout");
        requirePositive(cacheTtl, "cacheTtl");
        requirePositive(failureCacheTtl, "failureCacheTtl");
        if (!"https".equalsIgnoreCase(ratesUrl.getScheme()) && !isLoopbackHttp(ratesUrl)) {
            throw new IllegalArgumentException("Market rates URL must use HTTPS");
        }
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static boolean isLoopbackHttp(URI uri) {
        if (!"http".equalsIgnoreCase(uri.getScheme())) return false;
        var host = uri.getHost();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
