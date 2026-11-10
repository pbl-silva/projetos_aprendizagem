package br.com.spbank.endereco.adapter.out.http;

import br.com.spbank.endereco.application.model.PostalAddress;
import br.com.spbank.endereco.application.port.out.PostalCodeProvider;
import br.com.spbank.endereco.configuration.PostalCodeProperties;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class ViaCepPostalCodeProvider implements PostalCodeProvider {
    private static final int MAX_RESPONSE_LENGTH = 65_536;
    private final HttpClient http;
    private final JsonMapper json;
    private final PostalCodeProperties properties;

    public ViaCepPostalCodeProvider(JsonMapper json, PostalCodeProperties properties) {
        this(HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build(), json, properties);
    }

    ViaCepPostalCodeProvider(HttpClient http, JsonMapper json, PostalCodeProperties properties) {
        this.http = http;
        this.json = json;
        this.properties = properties;
    }

    @Override
    public Optional<PostalAddress> find(String postalCode) {
        if (!postalCode.matches("\\d{8}")) throw new IllegalArgumentException("Postal code must have eight digits");
        var endpoint = properties.baseUrl().resolve(postalCode + "/json/");
        var request = HttpRequest.newBuilder(endpoint)
                .timeout(properties.requestTimeout())
                .header("Accept", "application/json")
                .header("User-Agent", "SPBank-study-project/1.0")
                .GET()
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new PostalCodeProviderException("Unexpected ViaCEP status");
            if (response.body().length() > MAX_RESPONSE_LENGTH) throw new PostalCodeProviderException("ViaCEP response is too large");
            return parse(postalCode, response.body());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new PostalCodeProviderException("ViaCEP request interrupted", failure);
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof PostalCodeProviderException known) throw known;
            throw new PostalCodeProviderException("Could not obtain postal code", failure);
        }
    }

    private Optional<PostalAddress> parse(String requestedPostalCode, String body) {
        var payload = json.readValue(body, ViaCepPayload.class);
        if (Boolean.TRUE.equals(payload.erro()) || "true".equalsIgnoreCase(String.valueOf(payload.erro()))) {
            return Optional.empty();
        }
        var returnedPostalCode = text(payload.cep()).replaceAll("\\D", "");
        var state = text(payload.uf()).toUpperCase(Locale.ROOT);
        if (!returnedPostalCode.equals(requestedPostalCode)) {
            throw new PostalCodeProviderException("ViaCEP returned another postal code");
        }
        return Optional.of(new PostalAddress(returnedPostalCode, text(payload.logradouro()),
                text(payload.complemento()), text(payload.bairro()), text(payload.localidade()), state));
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private record ViaCepPayload(String cep, String logradouro, String complemento, String bairro,
                                 String localidade, String uf, Object erro) {}

    private static final class PostalCodeProviderException extends RuntimeException {
        private PostalCodeProviderException(String message) { super(message); }
        private PostalCodeProviderException(String message, Throwable cause) { super(message, cause); }
    }
}
