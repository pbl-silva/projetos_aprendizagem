package br.com.spbank.mercado.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.spbank.mercado.configuration.MarketRatesProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class BrasilApiMarketRatesProviderTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void mapsOnlyRequiredRatesInStableOrder() throws Exception {
        startServer(200, """
                [{"nome":"Selic","valor":13.75},{"nome":"CDI","valor":13.65},{"nome":"IPCA","valor":4.58}]
                """);

        var rates = provider().fetch();

        assertThat(rates).extracting(rate -> rate.name()).containsExactly("SELIC", "CDI");
        assertThat(rates).extracting(rate -> rate.annualPercent().toPlainString()).containsExactly("13.75", "13.65");
    }

    @Test
    void rejectsIncompleteProviderResponse() throws Exception {
        startServer(200, "[{\"nome\":\"Selic\",\"valor\":13.75}]");

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> provider().fetch());
    }

    private BrasilApiMarketRatesProvider provider() {
        var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/taxas/v1");
        var properties = new MarketRatesProperties(uri, Duration.ofSeconds(1), Duration.ofSeconds(2),
                Duration.ofMinutes(30), Duration.ofMinutes(1));
        return new BrasilApiMarketRatesProvider(HttpClient.newHttpClient(), JsonMapper.builder().build(), properties);
    }

    private void startServer(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/taxas/v1", exchange -> {
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
    }
}
