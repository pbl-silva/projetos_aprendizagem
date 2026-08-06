package com.biblioteca.biblioteca_api;

import com.biblioteca.biblioteca_api.ui.client.ApiClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiClientModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void deveLerUsuarioConformeContratoDaApi() throws Exception {
        String json = """
                {
                  "id": 1,
                  "nome": "Ana",
                  "email": "ana@email.com",
                  "cpf": "12345678901",
                  "tipoUsuario": "COMUM",
                  "dataCadastro": "2026-06-01"
                }
                """;

        ApiClient.Usuario usuario = objectMapper.readValue(json, ApiClient.Usuario.class);

        assertEquals("12345678901", usuario.cpf);
        assertEquals("COMUM", usuario.tipoUsuario);
        assertEquals("2026-06-01", usuario.dataCadastro);
    }

    @Test
    void deveLerEmprestimoConformeContratoDaApi() throws Exception {
        String json = """
                {
                  "id": 10,
                  "livro": {"id": 2, "titulo": "Clean Code"},
                  "usuario": {"id": 1, "nome": "Ana"},
                  "dataEmprestimo": "2026-06-01",
                  "dataDevolucaoPrevista": "2026-06-08",
                  "dataDevolucaoReal": null,
                  "status": "ATIVO",
                  "multaCalculada": 0.00,
                  "diasRestantes": 0
                }
                """;

        ApiClient.Emprestimo emprestimo = objectMapper.readValue(json, ApiClient.Emprestimo.class);

        assertNotNull(emprestimo.usuario);
        assertEquals("Ana", emprestimo.usuario.nome);
        assertNotNull(emprestimo.livro);
        assertEquals("Clean Code", emprestimo.livro.titulo);
        assertEquals("2026-06-08", emprestimo.dataDevolucaoPrevista);
        assertEquals(new BigDecimal("0.00"), emprestimo.multaCalculada);
    }
}
