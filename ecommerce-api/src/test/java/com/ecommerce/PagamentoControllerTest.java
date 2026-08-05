package com.ecommerce;

import com.ecommerce.api.dto.PagamentoDTO;
import com.ecommerce.api.enums.StatusPagamento;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.service.PagamentoService;
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
@DisplayName("Testes da API de Pagamentos")
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PagamentoService pagamentoService;

    private PagamentoDTO pagamentoDTO;

    @BeforeEach
    void setUp() {
        pagamentoDTO = PagamentoDTO.builder()
            .id(1L)
            .pedidoId(10L)
            .valor(150.0)
            .metodo("CARTAO")
            .status(StatusPagamento.PENDENTE)
            .build();
    }

    @Test
    @DisplayName("Deve criar pagamento")
    @WithMockUser
    void testCriar() throws Exception {
        when(pagamentoService.criar(any(PagamentoDTO.class))).thenReturn(pagamentoDTO);

        mockMvc.perform(post("/pagamentos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagamentoDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.metodo", is("CARTAO")));
    }

    @Test
    @DisplayName("Deve obter pagamento por ID")
    @WithMockUser
    void testObterPorId() throws Exception {
        when(pagamentoService.obterPorId(1L)).thenReturn(pagamentoDTO);

        mockMvc.perform(get("/pagamentos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedidoId", is(10)));
    }

    @Test
    @DisplayName("Deve retornar 404 para pagamento inexistente")
    @WithMockUser
    void testObterPorIdNaoEncontrado() throws Exception {
        when(pagamentoService.obterPorId(999L))
            .thenThrow(new RecursoNaoEncontradoException("Pagamento não encontrado"));

        mockMvc.perform(get("/pagamentos/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve listar pagamentos")
    @WithMockUser
    void testListarTodos() throws Exception {
        when(pagamentoService.listarTodos()).thenReturn(List.of(pagamentoDTO));

        mockMvc.perform(get("/pagamentos/todos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Deve atualizar pagamento")
    @WithMockUser
    void testAtualizar() throws Exception {
        when(pagamentoService.atualizar(eq(1L), any(PagamentoDTO.class))).thenReturn(pagamentoDTO);

        mockMvc.perform(put("/pagamentos/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagamentoDTO)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve atualizar status do pagamento")
    @WithMockUser
    void testAtualizarStatus() throws Exception {
        when(pagamentoService.atualizarStatus(1L, StatusPagamento.APROVADO)).thenReturn(pagamentoDTO);

        mockMvc.perform(patch("/pagamentos/1/status")
                .with(csrf())
                .param("status", "APROVADO"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve listar pagamentos com paginação")
    @WithMockUser
    void testListarComPaginacao() throws Exception {
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<PagamentoDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(pagamentoDTO), pageable, 1);
        when(pagamentoService.listarComPaginacao(0, 10, "id", "DESC")).thenReturn(page);

        mockMvc.perform(get("/pagamentos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Deve rejeitar criação de pagamento sem método")
    @WithMockUser
    void testCriarSemMetodo() throws Exception {
        PagamentoDTO invalido = PagamentoDTO.builder().pedidoId(10L).valor(50.0).build();

        mockMvc.perform(post("/pagamentos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalido)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve deletar pagamento")
    @WithMockUser
    void testDeletar() throws Exception {
        doNothing().when(pagamentoService).deletar(1L);

        mockMvc.perform(delete("/pagamentos/1").with(csrf()))
            .andExpect(status().isNoContent());
    }
}