package com.ecommerce;

import com.ecommerce.api.dto.ClienteDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.service.ClienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes da API de Clientes")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    private ClienteDTO clienteDTO;

    @BeforeEach
    void setUp() {
        clienteDTO = ClienteDTO.builder()
            .id(1L)
            .nome("João Silva")
            .email("joao@example.com")
            .cpf("12345678901")
            .ativo(true)
            .build();
    }

    @Test
    @DisplayName("Deve listar clientes")
    @WithMockUser
    void testListar() throws Exception {
        when(clienteService.listarTodos()).thenReturn(List.of(clienteDTO));

        mockMvc.perform(get("/clientes/todos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nome", is("João Silva")));
    }

    @Test
    @DisplayName("Deve obter cliente por ID")
    @WithMockUser
    void testObter() throws Exception {
        when(clienteService.obterPorId(1L)).thenReturn(clienteDTO);

        mockMvc.perform(get("/clientes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("joao@example.com")));
    }

    @Test
    @DisplayName("Deve retornar 404 para cliente inexistente")
    @WithMockUser
    void testObterNaoEncontrado() throws Exception {
        when(clienteService.obterPorId(999L))
            .thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado"));

        mockMvc.perform(get("/clientes/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve criar cliente")
    @WithMockUser
    void testCriar() throws Exception {
        when(clienteService.criar(any(ClienteDTO.class))).thenReturn(clienteDTO);

        mockMvc.perform(post("/clientes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clienteDTO)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve atualizar cliente")
    @WithMockUser
    void testAtualizar() throws Exception {
        when(clienteService.atualizar(eq(1L), any(ClienteDTO.class))).thenReturn(clienteDTO);

        mockMvc.perform(put("/clientes/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clienteDTO)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve listar clientes com paginação")
    @WithMockUser
    void testListarComPaginacao() throws Exception {
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<ClienteDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(clienteDTO), pageable, 1);
        when(clienteService.listarComPaginacao(0, 10, "id", "DESC")).thenReturn(page);

        mockMvc.perform(get("/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Deve rejeitar criação de cliente com email inválido")
    @WithMockUser
    void testCriarEmailInvalido() throws Exception {
        ClienteDTO invalido = ClienteDTO.builder()
            .nome("João Silva")
            .cpf("12345678901")
            .email("email-invalido")
            .build();

        mockMvc.perform(post("/clientes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalido)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve deletar cliente")
    @WithMockUser
    void testDeletar() throws Exception {
        doNothing().when(clienteService).deletar(1L);

        mockMvc.perform(delete("/clientes/1").with(csrf()))
            .andExpect(status().isNoContent());
    }
}