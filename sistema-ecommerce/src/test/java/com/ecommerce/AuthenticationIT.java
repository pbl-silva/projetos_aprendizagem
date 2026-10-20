package com.ecommerce;

import com.ecommerce.api.model.Usuario;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
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
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.admin.enabled=true",
        "app.admin.email=admin-it@teste.com",
        "app.admin.password=AdminIT123"
    }
)
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
        Response login = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "senha", "senha123"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("tipo", equalTo("Bearer"))
            .body("papel", equalTo(Usuario.Papel.USER.name()))
            .body("clienteId", notNullValue())
            .extract().response();
        String token = login.path("token");
        Integer clienteId = login.path("clienteId");

        // 3. O cliente consulta e atualiza apenas o próprio cadastro.
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/clientes/me")
        .then()
            .statusCode(200)
            .body("email", equalTo(email))
            .body("nome", equalTo("Usuário IT"));

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "nome", "Usuário IT Atualizado",
                "email", email,
                "cpf", "12345678901",
                "telefone", "11987654321",
                "endereco", "Rua de Teste, 100"
            ))
        .when()
            .put("/clientes/me")
        .then()
            .statusCode(200)
            .body("id", equalTo(clienteId))
            .body("nome", equalTo("Usuário IT Atualizado"));

        // 4. Sem token -> 401
        given()
        .when()
            .get("/produtos/todos")
        .then()
            .statusCode(401);

        // 5. Com token -> 200
        Integer produtoId = given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/produtos/todos")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(10))
            .extract().path("[0].id");

        // 6. O backend ignora um clienteId forjado e compra para a conta logada
        Response pedido = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "clienteId", 999999,
                "itens", List.of(Map.of("produtoId", produtoId, "quantidade", 1))
            ))
        .when()
            .post("/pedidos")
        .then()
            .statusCode(201)
            .body("clienteId", equalTo(clienteId))
            .body("clienteNome", equalTo("Usuário IT Atualizado"))
            .extract().response();

        // 7. O valor informado pelo cliente também é ignorado; vale o total do pedido
        Integer pedidoId = pedido.path("id");
        Float total = pedido.path("total");
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("pedidoId", pedidoId, "valor", 1, "metodo", "PIX"))
        .when()
            .post("/pagamentos")
        .then()
            .statusCode(201)
            .body("pedidoId", equalTo(pedidoId))
            .body("valor", equalTo(total));

        // 8. Cliente autenticado não pode manter o catálogo
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/produtos/999999")
        .then()
            .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @DisplayName("Administrador deve acessar manutenção do catálogo")
    void testAdministradorPodeManterCatalogo() {
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin-it@teste.com", "senha", "AdminIT123"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("papel", equalTo(Usuario.Papel.ADMIN.name()))
            .extract().path("token");

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/produtos/999999")
        .then()
            .statusCode(404);
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
    @DisplayName("Registro publico deve ignorar tentativa de criar administrador")
    void testRegistroPublicoNaoCriaAdministrador() {
        String email = "it-papel-" + UUID.randomUUID() + "@teste.com";

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "senha", "senha123",
                "nome", "Usuario comum",
                "papel", Usuario.Papel.ADMIN.name()
            ))
        .when()
            .post("/auth/registrar")
        .then()
            .statusCode(201)
            .body("papel", equalTo(Usuario.Papel.USER.name()));
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
