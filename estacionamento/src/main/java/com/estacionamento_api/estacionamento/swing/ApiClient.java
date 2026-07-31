package com.estacionamento_api.estacionamento.swing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Cliente HTTP mínimo para a interface Swing conversar com a API REST.
 * Usa java.net.http.HttpClient (built-in desde o Java 11) + Jackson,
 * que já está no classpath via spring-boot-starter-webmvc — nenhuma
 * dependência nova foi adicionada ao pom.xml.
 *
 * Desde que a API passou a exigir JWT em (quase) todo endpoint, este
 * cliente se autentica sozinho na primeira chamada (usuário admin padrão,
 * criado automaticamente na subida da API) e guarda o token em memória
 * para as chamadas seguintes. Isso é aceitável para um cliente desktop
 * de uso interno/exercício — uma aplicação real pediria login ao usuário
 * em vez de usar uma credencial fixa.
 */
public final class ApiClient {

    private static final String BASE_URL = "http://localhost:8080/api";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static volatile String token;

    private ApiClient() {
    }

    /** Lançada quando a API responde com status >= 400. */
    public static class ApiException extends RuntimeException {
        public final int status;

        public ApiException(int status, String body) {
            super(extrairMensagem(status, body));
            this.status = status;
        }

        private static String extrairMensagem(int status, String body) {
            try {
                Map<?, ?> mapa = MAPPER.readValue(body, Map.class);
                Object mensagem = mapa.get("mensagem");
                Object campos = mapa.get("campos");
                if (campos != null) {
                    return mensagem + " - " + campos;
                }
                if (mensagem != null) {
                    return mensagem.toString();
                }
            } catch (Exception ignorado) {
                // corpo não era um JSON de erro no formato esperado
            }
            return "Erro " + status + ": " + body;
        }
    }

    public static <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        HttpResponse<String> resposta = enviar("GET", path, null);
        return MAPPER.readValue(resposta.body(), type);
    }

    public static <T> T getList(String path, TypeReference<T> tipo) throws IOException, InterruptedException {
        HttpResponse<String> resposta = enviar("GET", path, null);
        return MAPPER.readValue(resposta.body(), tipo);
    }

    public static <T> T post(String path, Object corpo, Class<T> type) throws IOException, InterruptedException {
        String json = MAPPER.writeValueAsString(corpo);
        HttpResponse<String> resposta = enviar("POST", path, json);
        return MAPPER.readValue(resposta.body(), type);
    }

    /**
     * Garante que temos um token válido antes de qualquer chamada que não
     * seja o próprio login. Usa a credencial admin padrão, criada
     * automaticamente pela API na subida (veja EstacionamentoApiApplication).
     */
    private static synchronized void garantirAutenticacao() throws IOException, InterruptedException {
        if (token != null) {
            return;
        }

        Map<String, String> corpoLogin = Map.of("username", "admin", "senha", "admin123");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/auth/login"))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(corpoLogin)))
            .build();

        HttpResponse<String> resposta = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (resposta.statusCode() >= 400) {
            throw new ApiException(resposta.statusCode(), resposta.body());
        }

        Map<?, ?> corpoResposta = MAPPER.readValue(resposta.body(), Map.class);
        token = (String) corpoResposta.get("token");
    }

    private static HttpResponse<String> enviar(String metodo, String path, String jsonBody)
            throws IOException, InterruptedException {
        boolean rotaPublica = path.startsWith("/auth/");

        if (!rotaPublica) {
            garantirAutenticacao();
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

        if (!rotaPublica && token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        if ("POST".equals(metodo)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody));
        } else {
            builder.GET();
        }

        HttpResponse<String> resposta = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (resposta.statusCode() >= 400) {
            throw new ApiException(resposta.statusCode(), resposta.body());
        }

        return resposta;
    }
}
