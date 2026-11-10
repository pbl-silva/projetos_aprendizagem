package br.com.spbank.mercado.adapter.out.http;

import br.com.spbank.mercado.application.model.MarketRate;
import br.com.spbank.mercado.application.port.out.MarketRatesProvider;
import br.com.spbank.mercado.configuration.MarketRatesProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class BrasilApiMarketRatesProvider implements MarketRatesProvider {
    private static final List<String> REQUIRED_RATES = List.of("SELIC", "CDI");
    private final HttpClient http;
    private final JsonMapper json;
    private final MarketRatesProperties properties;

    public BrasilApiMarketRatesProvider(JsonMapper json, MarketRatesProperties properties) {
        this(HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build(), json, properties);
    }

    BrasilApiMarketRatesProvider(HttpClient http, JsonMapper json, MarketRatesProperties properties) {
        this.http = http;
        this.json = json;
        this.properties = properties;
    }

    @Override
    public List<MarketRate> fetch() {
        var request = HttpRequest.newBuilder(properties.ratesUrl())
                .timeout(properties.requestTimeout())
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new MarketRatesProviderException("Unexpected market rates status: " + response.statusCode());
            }
            return parse(response.body());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new MarketRatesProviderException("Market rates request interrupted", failure);
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof MarketRatesProviderException known) throw known;
            throw new MarketRatesProviderException("Could not obtain market rates", failure);
        }
    }

    private List<MarketRate> parse(String body) {
        var found = new LinkedHashMap<String, BigDecimal>();
        for (var item : json.readValue(body, MarketRatePayload[].class)) {
            if (item.nome() == null || item.valor() == null || item.valor().signum() < 0) continue;
            var canonicalName = item.nome().trim().toUpperCase(Locale.ROOT);
            if (REQUIRED_RATES.contains(canonicalName)) found.put(canonicalName, item.valor());
        }
        if (!found.keySet().containsAll(REQUIRED_RATES)) {
            throw new MarketRatesProviderException("Required rates are missing from provider response");
        }
        return REQUIRED_RATES.stream().map(name -> new MarketRate(name, found.get(name))).toList();
    }

    private record MarketRatePayload(String nome, BigDecimal valor) {}

    private static final class MarketRatesProviderException extends RuntimeException {
        private MarketRatesProviderException(String message) { super(message); }
        private MarketRatesProviderException(String message, Throwable cause) { super(message, cause); }
    }
}
