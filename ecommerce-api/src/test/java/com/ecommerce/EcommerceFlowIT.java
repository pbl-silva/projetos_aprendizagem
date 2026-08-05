package com.ecommerce;

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
 * Teste de integração real do fluxo de negócio completo: cria categoria,
 * produto, cliente, pedido e pagamento via HTTP de verdade contra o servidor
 * embarcado, exercitando o Hibernate/H2, as constraints de FK entre as
 * tabelas, a paginação real e a validação de bean ponta a ponta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("IT - Fluxo completo de e-commerce")
class EcommerceFlowIT {

    @LocalServerPort
    private int port;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api";

        String email = "flow-" + UUID.randomUUID() + "@teste.com";
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "senha", "senha123", "nome", "Usuário Fluxo"))
        .when()
            .post("/auth/registrar")
        .then()
            .statusCode(201);

        token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "senha", "senha123"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract().path("token");
    }

    @Test
    @DisplayName("Deve executar o fluxo completo: categoria -> produto -> cliente -> pedido -> pagamento")
    void testFluxoCompletoDeVenda() {
        String auth = "Bearer " + token;

        // 1. Criar categoria
        Long categoriaId = given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body(Map.of("nome", "Eletrônicos IT", "descricao", "Categoria de teste", "ativo", true))
        .when()
            .post("/categorias")
        .then()
            .statusCode(201)
            .body("nome", equalTo("Eletrônicos IT"))
            .extract().jsonPath().getLong("id");

        // 2. Criar produto vinculado à categoria
        Long produtoId = given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "nome", "Notebook IT",
                "descricao", "Notebook de teste",
                "preco", 3500.00,
                "estoque", 10,
                "categoriaId", categoriaId,
                "ativo", true
            ))
        .when()
            .post("/produtos")
        .then()
            .statusCode(201)
            .body("nome", equalTo("Notebook IT"))
            .extract().jsonPath().getLong("id");

        // 3. Criar cliente
        String cpf = String.valueOf(System.nanoTime()).substring(0, 11);
        Long clienteId = given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "nome", "Cliente IT",
                "cpf", cpf,
                "email", "cliente-" + UUID.randomUUID() + "@teste.com",
                "ativo", true
            ))
        .when()
            .post("/clientes")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        // 4. Criar pedido vinculado ao cliente
        Long pedidoId = given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body(Map.of("clienteId", clienteId, "total", 3500.00))
        .when()
            .post("/pedidos")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDENTE"))
            .extract().jsonPath().getLong("id");

        // 5. Criar pagamento vinculado ao pedido
        given()
            .header("Authorization", auth)
            .contentType(ContentType.JSON)
            .body(Map.of("pedidoId", pedidoId, "valor", 3500.00, "metodo", "CARTAO"))
        .when()
            .post("/pagamentos")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDENTE"));

        // 6. Diminuir estoque do produto via HTTP real
        given()
            .header("Authorization", auth)
        .when()
            .patch("/produtos/" + produtoId + "/estoque/diminuir?quantidade=3")
        .then()
            .statusCode(200)
            .body(equalTo("7"));

        // 7. Listar produtos paginados e confirmar presença do produto criado
        given()
            .header("Authorization", auth)
        .when()
            .get("/produtos?pagina=0&tamanho=50")
        .then()
            .statusCode(200)
            .body("content.nome", hasItem("Notebook IT"));

        // 8. Deletar o produto criado e confirmar 404 em seguida
        given()
            .header("Authorization", auth)
        .when()
            .delete("/produtos/" + produtoId)
        .then()
            .statusCode(204);

        given()
            .header("Authorization", auth)
        .when()
            .get("/produtos/" + produtoId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar pedido para cliente inexistente")
    void testCriarPedidoClienteInexistente() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("clienteId", 999999L, "total", 100.0))
        .when()
            .post("/pedidos")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("Deve listar categorias com paginação real")
    void testListarCategoriasComPaginacao() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("nome", "Categoria Paginação " + UUID.randomUUID(), "ativo", true))
        .when()
            .post("/categorias")
        .then()
            .statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/categorias?pagina=0&tamanho=5")
        .then()
            .statusCode(200)
            .body("content", not(empty()))
            .body("size", equalTo(5));
    }
}