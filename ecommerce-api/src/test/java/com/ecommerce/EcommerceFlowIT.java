package com.ecommerce;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Teste de integração real do fluxo de negócio completo: cria categoria,
 * produto, cliente, pedido e pagamento via HTTP de verdade contra o servidor
 * embarcado, exercitando o Hibernate/H2, as constraints de FK entre as
 * tabelas, a paginação real e a validação de bean ponta a ponta.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.admin.enabled=true",
        "app.admin.email=admin-flow@teste.com",
        "app.admin.password=AdminFlow123"
    }
)
@DisplayName("IT - Fluxo completo de e-commerce")
class EcommerceFlowIT {

    @LocalServerPort
    private int port;

    private String tokenCliente;
    private String tokenAdmin;
    private Long clienteId;

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

        var loginCliente = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "senha", "senha123"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract().response();

        tokenCliente = loginCliente.path("token");
        clienteId = loginCliente.jsonPath().getLong("clienteId");

        tokenAdmin = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin-flow@teste.com", "senha", "AdminFlow123"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract().path("token");
    }

    @Test
    @DisplayName("Deve executar o fluxo completo: categoria -> produto -> cliente -> pedido -> pagamento")
    void testFluxoCompletoDeVenda() {
        String authAdmin = "Bearer " + tokenAdmin;
        String authCliente = "Bearer " + tokenCliente;

        // 1. Criar categoria
        Long categoriaId = given()
            .header("Authorization", authAdmin)
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
            .header("Authorization", authAdmin)
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

        // 3. O cliente compra usando apenas a conta vinculada ao seu login.
        Long pedidoId = given()
            .header("Authorization", authCliente)
            .contentType(ContentType.JSON)
            .body(Map.of("clienteId", clienteId,
                "itens", List.of(Map.of("produtoId", produtoId, "quantidade", 1)),
                "modalidadeFrete", "PADRAO",
                "cepEntrega", "01001000",
                "enderecoEntrega", "Praça da Sé, 1 - São Paulo/SP"))
        .when()
            .post("/pedidos")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDENTE"))
            .body("subtotal", equalTo(3500.0f))
            .body("frete", equalTo(0.0f))
            .body("modalidadeFrete", equalTo("PADRAO"))
            .body("total", equalTo(3500.0f))
            .body("itens", hasSize(1))
            .extract().jsonPath().getLong("id");

        // 4. O cliente paga o próprio pedido.
        Long pagamentoId = given()
            .header("Authorization", authCliente)
            .contentType(ContentType.JSON)
            .body(Map.of("pedidoId", pedidoId, "valor", 3500.00, "metodo", "CARTAO_CREDITO"))
        .when()
            .post("/pagamentos")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDENTE"))
            .body("metodo", equalTo("CARTAO_CREDITO"))
            .extract().jsonPath().getLong("id");

        // 5. Trocar o método atualiza o pagamento pendente, sem criar duplicata.
        given()
            .header("Authorization", authCliente)
            .contentType(ContentType.JSON)
            .body(Map.of("pedidoId", pedidoId, "valor", 1.00, "metodo", "BOLETO"))
        .when()
            .post("/pagamentos")
        .then()
            .statusCode(201)
            .body("id", equalTo(pagamentoId.intValue()))
            .body("valor", equalTo(3500.0f))
            .body("metodo", equalTo("BOLETO"));

        given()
            .header("Authorization", authCliente)
        .when()
            .get("/pagamentos/meus")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1));

        // 6. Somente o administrador faz manutenção manual do estoque.
        given()
            .header("Authorization", authAdmin)
        .when()
            .patch("/produtos/" + produtoId + "/estoque/diminuir?quantidade=3")
        .then()
            .statusCode(200)
            .body(equalTo("6"));

        // 7. O cliente pode consultar o catálogo.
        given()
            .header("Authorization", authCliente)
        .when()
            .get("/produtos?pagina=0&tamanho=50")
        .then()
            .statusCode(200)
            .body("content.nome", hasItem("Notebook IT"));

        // 8. Consultar o produto usado no pedido e confirmar o estoque final.
        given()
            .header("Authorization", authCliente)
        .when()
            .get("/produtos/" + produtoId)
        .then()
            .statusCode(200)
            .body("estoque", equalTo(6));
    }

    @Test
    @DisplayName("Cliente não pode comprar em nome de outro cliente")
    void testClienteIdForjadoEhIgnorado() {
        Long produtoId = given()
            .header("Authorization", "Bearer " + tokenCliente)
        .when()
            .get("/produtos/todos")
        .then()
            .statusCode(200)
            .extract().jsonPath().getLong("[0].id");

        given()
            .header("Authorization", "Bearer " + tokenCliente)
            .contentType(ContentType.JSON)
            .body(Map.of("clienteId", 999999L,
                "itens", List.of(Map.of("produtoId", produtoId, "quantidade", 1))))
        .when()
            .post("/pedidos")
        .then()
            .statusCode(201)
            .body("clienteId", equalTo(clienteId.intValue()));
    }

    @Test
    @DisplayName("Deve listar categorias com paginação real")
    void testListarCategoriasComPaginacao() {
        given()
            .header("Authorization", "Bearer " + tokenAdmin)
            .contentType(ContentType.JSON)
            .body(Map.of("nome", "Categoria Paginação " + UUID.randomUUID(), "ativo", true))
        .when()
            .post("/categorias")
        .then()
            .statusCode(201);

        given()
            .header("Authorization", "Bearer " + tokenCliente)
        .when()
            .get("/categorias?pagina=0&tamanho=5")
        .then()
            .statusCode(200)
            .body("content", not(empty()))
            .body("size", equalTo(5));
    }
}
