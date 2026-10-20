package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.repository.UsuarioRepository;
import com.estacionamento_api.estacionamento.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MinhaContaIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioService service;
    @Autowired private UsuarioRepository usuarios;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void ambosPerfisPodemAtualizarDadosSemMudarPermissao() throws Exception {
        for (String role : new String[] { "ADMIN", "OPERADOR" }) {
            String nome = "conta-" + role;
            service.registrar(nome, "senha123", role);
            String token = login(nome, "senha123");
            String body = json.writeValueAsString(Map.of("username", nome, "nome", "Ana Silva",
                "email", "ana@exemplo.com", "telefone", "11999999999", "senhaAtual", "senha123",
                "role", "ADMIN", "id", 999));
            var result = mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token)
                    .contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn();
            JsonNode response = json.readTree(result.getResponse().getContentAsString());
            assertEquals(role, response.get("usuario").get("role").asText());
            assertEquals("Ana Silva", response.get("usuario").get("nome").asText());
            assertFalse(response.get("usuario").has("senha"));
            assertEquals("ana@exemplo.com", service.obterUsuario(nome).email());
            assertNotEquals(999L, service.obterUsuario(nome).id());
        }
    }

    @Test
    void alteracaoDeNomeDeAcessoMantemContaEInvalidaTokenAntigoMesmoSeNomeForReutilizado() throws Exception {
        service.registrar("nome-antigo", "senha123", "OPERADOR");
        Long id = service.obterUsuario("nome-antigo").id();
        String token = login("nome-antigo", "senha123");
        JsonNode result = atualizar(token, "nome-novo", "senha123");
        assertEquals(id.longValue(), result.get("usuario").get("id").asLong());
        String novoToken = result.get("token").asText();
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + novoToken)).andExpect(status().isOk());
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        service.registrar("nome-antigo", "senha123", "ADMIN");
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        assertNotNull(login("nome-novo", "senha123"));
    }

    @Test
    void senhaAtualErradaOuUsuarioDuplicadoNaoAlteramConta() throws Exception {
        service.registrar("minha-conta", "senha123", "OPERADOR");
        service.registrar("outra-conta", "senha123", "ADMIN");
        String token = login("minha-conta", "senha123");
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", "outro-nome", "senhaAtual", "incorreta"))))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", "outra-conta", "senhaAtual", "senha123"))))
            .andExpect(status().isConflict());
        assertEquals("OPERADOR", service.obterUsuario("minha-conta").role());
        assertEquals("ADMIN", service.obterUsuario("outra-conta").role());
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void trocaDeSenhaInvalidaTodasAsSessoesAntigasEPreservaSessaoRenovada() throws Exception {
        service.registrar("troca-senha", "senha123", "OPERADOR");
        String token = login("troca-senha", "senha123");
        var result = mvc.perform(put("/auth/me/senha").header("Authorization", "Bearer " + token)
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                    "senhaAtual", "senha123", "novaSenha", "senhaNova123", "confirmacaoSenha", "senhaNova123"))))
            .andExpect(status().isOk()).andReturn();
        String novoToken = json.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + novoToken)).andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"username\":\"troca-senha\",\"senha\":\"senha123\"}"))
            .andExpect(status().isUnauthorized());
        assertNotNull(login("troca-senha", "senhaNova123"));
        assertNotEquals("senhaNova123", usuarios.findByUsername("troca-senha").orElseThrow().getSenha());
    }

    @Test
    void senhasDiferentesOuSenhaAtualErradaNaoMudamSenha() throws Exception {
        service.registrar("senha-invalida", "senha123", "OPERADOR");
        String token = login("senha-invalida", "senha123");
        for (var body : new Map[] {
                Map.of("senhaAtual", "senhaErrada", "novaSenha", "novaSenha123", "confirmacaoSenha", "novaSenha123"),
                Map.of("senhaAtual", "senha123", "novaSenha", "novaSenha123", "confirmacaoSenha", "diferente123") }) {
            mvc.perform(put("/auth/me/senha").header("Authorization", "Bearer " + token)
                    .contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        }
        assertNotNull(login("senha-invalida", "senha123"));
    }

    @Test
    void deveValidarEmailSenhaEDadosEExigirAutenticacao() throws Exception {
        service.registrar("validacao-conta", "senha123", "OPERADOR");
        String token = login("validacao-conta", "senha123");
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"username\":\"validacao-conta\",\"email\":\"invalido\",\"senhaAtual\":\"senha123\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/auth/me/senha").header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"senhaAtual\":\"senha123\",\"novaSenha\":\"123\",\"confirmacaoSenha\":\"123\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/auth/me").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/auth/me/senha").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void renomearAdministradorNaoRecriaContaInicial() throws Exception {
        usuarios.deleteAll();
        service.registrar("admin", "senha123", "ADMIN");
        atualizar(login("admin", "senha123"), "novo-admin", "senha123");
        new EstacionamentoApiApplication().inicializarUsuarioAdmin(service, "admin", "senhaInicial123").run();
        assertFalse(usuarios.existsByUsername("admin"));
        assertEquals("ADMIN", service.obterUsuario("novo-admin").role());
    }

    private JsonNode atualizar(String token, String username, String senha) throws Exception {
        var result = mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token)
                .contentType("application/json").content(json.writeValueAsString(Map.of("username", username, "senhaAtual", senha))))
            .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String login(String username, String senha) throws Exception {
        var result = mvc.perform(post("/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", username, "senha", senha))))
            .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
