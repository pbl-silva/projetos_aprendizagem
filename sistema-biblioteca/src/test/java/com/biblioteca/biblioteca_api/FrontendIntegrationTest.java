package com.biblioteca.biblioteca_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test
    void devePublicarInterfaceEIdentificarBiblioteca() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("Histórias feitas")));
        for (String asset : new String[] { "/app.js", "/styles.css", "/favicon.svg" }) mvc.perform(get(asset)).andExpect(status().isOk());
        mvc.perform(get("/api/sistema")).andExpect(status().isOk()).andExpect(jsonPath("$.aplicacao").value("biblioteca"));
    }

    @Test
    void relatorioDeveSerPdfValido() throws Exception {
        var result = mvc.perform(get("/api/relatorios/usuario/1/emprestimos"))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn();
        try (var doc = org.apache.pdfbox.Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
            assertTrue(text.contains("RELATÓRIO DE EMPRÉSTIMOS"));
            assertTrue(text.contains("Ananias"));
        }
    }

    @Test
    void encerramentoDeveRecusarChamadasSemChaveDoIniciador() throws Exception {
        mvc.perform(post("/api/sistema/encerrar")).andExpect(status().isForbidden());
        mvc.perform(post("/api/sistema/encerrar").header("X-Iniciador-Token", "incorreta"))
            .andExpect(status().isForbidden());
    }
}
