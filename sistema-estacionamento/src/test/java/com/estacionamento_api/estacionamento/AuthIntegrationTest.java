package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.dto.LoginRequestDTO;
import com.estacionamento_api.estacionamento.dto.LoginResponseDTO;
import com.estacionamento_api.estacionamento.dto.RegistroDTO;
import com.estacionamento_api.estacionamento.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do mecanismo real de autenticação — sem
 * @WithMockUser aqui de propósito, já que o objetivo é validar o
 * fluxo de JWT de ponta a ponta (registro -> login -> uso do token).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Testes de integração da autenticação JWT")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Deve registrar um novo usuário")
    void testRegistrarUsuario() throws Exception {
        String token = adminToken();
        RegistroDTO dto = RegistroDTO.builder()
            .username("novousuario")
            .senha("senha123")
            .build();

        mockMvc.perform(post("/auth/registrar")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Não deve permitir registrar o mesmo username duas vezes")
    void testNaoDevePermitirUsernameDuplicado() throws Exception {
        String token = adminToken();
        RegistroDTO dto = RegistroDTO.builder()
            .username("duplicado")
            .senha("senha123")
            .build();

        mockMvc.perform(post("/auth/registrar")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/registrar")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve fazer login e receber um token válido")
    void testLoginComCredenciaisCorretas() throws Exception {
        registrarUsuario("usuariologin", "senha123");

        LoginRequestDTO login = LoginRequestDTO.builder()
            .username("usuariologin")
            .senha("senha123")
            .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        LoginResponseDTO resposta = objectMapper.readValue(
            result.getResponse().getContentAsString(), LoginResponseDTO.class);

        assertNotNull(resposta.getToken());
        assertEquals("Bearer", resposta.getTipo());
        assertEquals("usuariologin", resposta.getUsername());
    }

    @Test
    @DisplayName("Não deve autenticar com senha errada")
    void testLoginComSenhaErradaRetorna401() throws Exception {
        registrarUsuario("usuariosenhaerrada", "senhacerta");

        LoginRequestDTO login = LoginRequestDTO.builder()
            .username("usuariosenhaerrada")
            .senha("senhaerrada")
            .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Não deve autenticar usuário inexistente")
    void testLoginComUsuarioInexistenteRetorna401() throws Exception {
        LoginRequestDTO login = LoginRequestDTO.builder()
            .username("naoexiste")
            .senha("qualquersenha")
            .build();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve bloquear acesso a endpoint protegido sem token")
    void testAcessoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/vagas"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve permitir acesso a endpoint protegido com token válido")
    void testAcessoComTokenValido() throws Exception {
        registrarUsuario("usuariocomtoken", "senha123");
        String token = login("usuariocomtoken", "senha123");

        mockMvc.perform(get("/vagas")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve bloquear acesso com token inválido")
    void testAcessoComTokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/vagas")
                .header("Authorization", "Bearer token.invalido.aqui"))
            .andExpect(status().isUnauthorized());
    }

    private void registrarUsuario(String username, String senha) throws Exception {
        usuarioService.registrar(username, senha, "OPERADOR");
    }

    private String adminToken() throws Exception {
        usuarioService.registrar("admin-teste", "senhaAdmin123", "ADMIN");
        return login("admin-teste", "senhaAdmin123");
    }

    @Test
    void deveBloquearCadastroSemAutenticacao() throws Exception {
        mockMvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"invasor\",\"senha\":\"senha123\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isUnauthorized());
        assertFalse(usuarioService.existeUsuario("invasor"));
    }

    @Test
    void funcionarioNaoPodeCriarContasNemListarEquipe() throws Exception {
        registrarUsuario("funcionario", "senha123");
        String token = login("funcionario", "senha123");
        for (String role : new String[] { "OPERADOR", "ADMIN" }) {
            mockMvc.perform(post("/auth/registrar").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegistroDTO.builder()
                        .username("contabloqueada").senha("senha123").role(role).build())))
                .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
        assertFalse(usuarioService.existeUsuario("contabloqueada"));
    }

    @Test
    void administradorPodeCriarOutroAdministradorQueTambemPodeCadastrar() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/auth/registrar").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegistroDTO.builder()
                    .username("novo-admin").senha("senha123").role("ADMIN").build())))
            .andExpect(status().isCreated());
        String novoToken = login("novo-admin", "senha123");
        mockMvc.perform(post("/auth/registrar").header("Authorization", "Bearer " + novoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegistroDTO.builder()
                    .username("novo-funcionario").senha("senha123").role("OPERADOR").build())))
            .andExpect(status().isCreated());
        assertEquals("OPERADOR", usuarioService.obterUsuario("novo-funcionario").role());
    }

    @Test
    void deveRejeitarPerfilInvalidoOuNulo() throws Exception {
        String token = adminToken();
        for (String role : new String[] { "SUPER_ADMIN", "", null }) {
            mockMvc.perform(post("/auth/registrar").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RegistroDTO.builder()
                        .username("perfil-invalido").senha("senha123").role(role).build())))
                .andExpect(status().isBadRequest());
        }
        assertFalse(usuarioService.existeUsuario("perfil-invalido"));
    }

    @Test
    void deveConsultarPerfilAtualSemExporSenha() throws Exception {
        registrarUsuario("operador-atual", "senha123");
        String token = login("operador-atual", "senha123");
        MvcResult result = mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn();
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("OPERADOR", json.get("role").asText());
        assertEquals("operador-atual", json.get("username").asText());
        assertFalse(json.has("senha"));
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void administradorPodeListarEquipeSemExporCredenciais() throws Exception {
        String token = adminToken();
        MvcResult result = mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn();
        var users = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(users.size() > 0);
        users.forEach(user -> { assertFalse(user.has("senha")); assertFalse(user.has("password")); });
        mockMvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
    }

    private String login(String username, String senha) throws Exception {
        LoginRequestDTO dto = LoginRequestDTO.builder().username(username).senha(senha).build();
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andReturn();

        LoginResponseDTO resposta = objectMapper.readValue(
            result.getResponse().getContentAsString(), LoginResponseDTO.class);
        return resposta.getToken();
    }
}
