package com.estacionamento_api.estacionamento;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendAccessTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void deveServirLoginEArquivosWebSemAutenticacao() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8).contains("Painel de operação")));
        mvc.perform(get("/app.js")).andExpect(status().isOk())
            .andExpect(content().string(containsString("/entradas/ativas")));
        mvc.perform(get("/styles.css")).andExpect(status().isOk());
        mvc.perform(get("/favicon.svg")).andExpect(status().isOk());
    }

    @Test
    void deveManterDadosEOperacoesProtegidosMesmoComLoginPublico() throws Exception {
        for (String path : new String[] { "/veiculos", "/vagas", "/entradas/ativas",
                "/saidas/previa/1", "/saidas/recibo/1", "/relatorios/ocupacao", "/relatorios/pdf" }) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/entradas").contentType("application/json").content("{\"veiculoId\":1}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/saidas").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
