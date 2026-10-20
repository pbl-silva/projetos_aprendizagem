package com.ecommerce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve disponibilizar o painel web sem autenticação")
    void deveDisponibilizarPaginaInicial() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Pague Mais")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-view=\"carrinho\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-view=\"conta\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("autocomplete=\"off\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"login-email\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"login-password\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("autocomplete=\"new-password\"")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("teste123"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("API conectada"))));
    }

    @Test
    @DisplayName("Deve disponibilizar o JavaScript do painel sem autenticação")
    void deveDisponibilizarJavascript() throws Exception {
        mockMvc.perform(get("/app.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("const state")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("paymentMethodLabel")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("clearLoginForm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("prepareEmptyLoginForm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("renderAccount")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirmar entrega e ir para pagamento")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("openForm(\"pedido\", null"))));
    }
}
