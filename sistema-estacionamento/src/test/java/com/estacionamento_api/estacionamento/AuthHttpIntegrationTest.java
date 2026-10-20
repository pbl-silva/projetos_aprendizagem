package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.service.UsuarioService;
import com.estacionamento_api.estacionamento.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthHttpIntegrationTest {
    @Value("${local.server.port}")
    private int port;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void servidorDeveResponder403SemRedirecionarNegacaoParaErroDeAutenticacao() throws Exception {
        String username = "http-" + UUID.randomUUID();
        usuarioService.registrar(username, "senhaTeste123", "OPERADOR");
        try (HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            var login = client.send(HttpRequest.newBuilder(uri("/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"" + username
                    + "\",\"senha\":\"senhaTeste123\"}")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, login.statusCode());
            String token = new ObjectMapper().readTree(login.body()).get("token").asText();
            var denied = client.send(HttpRequest.newBuilder(uri("/usuarios"))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, denied.statusCode());
            assertEquals(403, new ObjectMapper().readTree(denied.body()).get("status").asInt());
            var create = client.send(HttpRequest.newBuilder(uri("/auth/registrar"))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"bloqueado-http\",\"senha\":\"senha123\",\"role\":\"ADMIN\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, create.statusCode());
            assertFalse(usuarioRepository.existsByUsername("bloqueado-http"));
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + "/api" + path);
    }
}
