package br.com.spbank.endereco.configuration;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spbank.postal-code")
public record PostalCodeProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration successCacheTtl,
        Duration notFoundCacheTtl,
        Duration failureCacheTtl,
        int maxCacheEntries
) {
    public PostalCodeProperties {
        Objects.requireNonNull(baseUrl, "baseUrl");
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(requestTimeout, "requestTimeout");
        requirePositive(successCacheTtl, "successCacheTtl");
        requirePositive(notFoundCacheTtl, "notFoundCacheTtl");
        requirePositive(failureCacheTtl, "failureCacheTtl");
        if (maxCacheEntries < 1 || maxCacheEntries > 10_000) {
            throw new IllegalArgumentException("maxCacheEntries must be between 1 and 10000");
        }
        if (!baseUrl.getPath().endsWith("/")) {
            throw new IllegalArgumentException("Postal code base URL must end with a slash");
        }
        if (!"https".equalsIgnoreCase(baseUrl.getScheme()) && !isLoopbackHttp(baseUrl)) {
            throw new IllegalArgumentException("Postal code base URL must use HTTPS");
        }
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " must be positive");
    }

    private static boolean isLoopbackHttp(URI uri) {
        if (!"http".equalsIgnoreCase(uri.getScheme())) return false;
        var host = uri.getHost();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
