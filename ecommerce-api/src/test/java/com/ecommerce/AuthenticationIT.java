package com.ecommerce;

import com.ecommerce.api.model.Usuario;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Teste de integração real: sobe o servidor embarcado (porta aleatória) e faz
 * chamadas HTTP de verdade, exercitando o filtro de segurança, o JJWT, a
 * validação de bean, o Hibernate/H2 e a serialização JSON de ponta a ponta -
 * nada disso é mockado, ao contrário dos testes com @MockitoBean.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("IT - Fluxo de autenticação")
class AuthenticationIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    @DisplayName("Deve registrar, logar e acessar endpoint protegido com o token JWT")
    void testFluxoCompletoDeAutenticacao() {
        String email = "it-" + UUID.randomUUID() + "@teste.com";

        // 1. Registrar
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "senha", "senha123",
                "nome", "Usuário IT",
                "papel", Usuario.Papel.USER.name()
            ))
        .when()
            .post("/auth/registrar")
        .then()
            .statusCode(201)
            .body("email", equalTo(email));

        // 2. Logar
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "senha", "senha123"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("tipo", equalTo("Bearer"))
            .extract().path("token");

        // 3. Sem token -> 401
        given()
        .when()
            .get("/produtos/todos")
        .then()
            .statusCode(401);

        // 4. Com token -> 200
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/produtos/todos")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Deve rejeitar login com senha incorreta")
    void testLoginSenhaIncorreta() {
        String email = "it-senha-" + UUID.randomUUID() + "@teste.com";

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "senha", "senha123", "nome", "Usuário IT"))
        .when()
            .post("/auth/registrar")
        .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "senha", "senhaErrada"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Deve rejeitar registro com email inválido (bean validation ponta a ponta)")
    void testRegistrarEmailInvalido() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "nao-e-email", "senha", "senha123", "nome", "Usuário IT"))
        .when()
            .post("/auth/registrar")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Swagger UI e API docs devem ser acessíveis sem autenticação")
    void testSwaggerAcessivelSemAuth() {
        given()
        .when()
            .get("/v3/api-docs")
        .then()
            .statusCode(200);
    }
}