package com.ecommerce;

import com.ecommerce.api.dto.LoginRequest;
import com.ecommerce.api.dto.LoginResponse;
import com.ecommerce.api.dto.UsuarioDTO;
import com.ecommerce.api.model.Usuario;
import com.ecommerce.api.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes da API de Autenticação")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve registrar novo usuário sem exigir autenticação")
    void testRegistrar() throws Exception {
        UsuarioDTO dto = UsuarioDTO.builder()
            .email("novo@teste.com")
            .senha("senha123")
            .nome("Novo Usuário")
            .papel(Usuario.Papel.USER)
            .build();

        when(usuarioService.registrar(any(UsuarioDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/auth/registrar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email", is("novo@teste.com")));
    }

    @Test
    @DisplayName("Deve realizar login sem exigir autenticação")
    void testLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("usuario@teste.com");
        request.setSenha("senha123");

        LoginResponse response = LoginResponse.of("token-fake", 1L, "usuario@teste.com", "Usuário Teste");

        when(usuarioService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", is("token-fake")))
            .andExpect(jsonPath("$.tipo", is("Bearer")));
    }
}