package br.com.spbank.endereco.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.spbank.endereco.configuration.PostalCodeProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ViaCepPostalCodeProviderTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void mapsViaCepFieldsAndKeepsTheRequestedPostalCode() throws Exception {
        startServer("""
                {"cep":"01001-000","logradouro":"Praça da Sé","complemento":"lado ímpar",
                 "bairro":"Sé","localidade":"São Paulo","uf":"SP"}
                """);

        var result = provider().find("01001000").orElseThrow();

        assertThat(result.postalCode()).isEqualTo("01001000");
        assertThat(result.street()).isEqualTo("Praça da Sé");
        assertThat(result.district()).isEqualTo("Sé");
        assertThat(result.city()).isEqualTo("São Paulo");
        assertThat(result.state()).isEqualTo("SP");
    }

    @Test
    void mapsViaCepErrorToNotFound() throws Exception {
        startServer("{\"erro\":true}");

        assertThat(provider().find("99999999")).isEmpty();
    }

    @Test
    void rejectsAResponseForAnotherPostalCode() throws Exception {
        startServer("""
                {"cep":"01310-100","logradouro":"Avenida Paulista","complemento":"",
                 "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP"}
                """);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> provider().find("01001000"));
    }

    private ViaCepPostalCodeProvider provider() {
        var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/ws/");
        var properties = new PostalCodeProperties(uri, Duration.ofSeconds(1), Duration.ofSeconds(2),
                Duration.ofHours(24), Duration.ofMinutes(5), Duration.ofMinutes(1), 100);
        return new ViaCepPostalCodeProvider(HttpClient.newHttpClient(), JsonMapper.builder().build(), properties);
    }

    private void startServer(String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ws/", exchange -> {
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
    }
}
