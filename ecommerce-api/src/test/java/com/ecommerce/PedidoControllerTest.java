package com.ecommerce;

import com.ecommerce.api.dto.PedidoDTO;
import com.ecommerce.api.dto.ItemPedidoDTO;
import com.ecommerce.api.enums.StatusPedido;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@DisplayName("Testes da API de Pedidos")
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PedidoService pedidoService;

    private PedidoDTO pedidoDTO;

    @BeforeEach
    void setUp() {
        pedidoDTO = PedidoDTO.builder()
            .id(1L)
            .clienteId(10L)
            .clienteNome("Cliente Teste")
            .total(150.0)
            .status(StatusPedido.PENDENTE)
            .itens(List.of(ItemPedidoDTO.builder().produtoId(5L).quantidade(1).build()))
            .build();
    }

    @Test
    @DisplayName("Deve listar pedidos")
    @WithMockUser(roles = "ADMIN")
    void testListar() throws Exception {
        when(pedidoService.listarTodos()).thenReturn(List.of(pedidoDTO));

        mockMvc.perform(get("/pedidos/todos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Deve obter pedido por ID")
    @WithMockUser(roles = "ADMIN")
    void testObter() throws Exception {
        when(pedidoService.obterPorId(1L)).thenReturn(pedidoDTO);

        mockMvc.perform(get("/pedidos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clienteId", is(10)))
            .andExpect(jsonPath("$.clienteNome", is("Cliente Teste")));
    }

    @Test
    @DisplayName("Deve retornar 404 para pedido inexistente")
    @WithMockUser(roles = "ADMIN")
    void testObterNaoEncontrado() throws Exception {
        when(pedidoService.obterPorId(999L))
            .thenThrow(new RecursoNaoEncontradoException("Pedido não encontrado"));

        mockMvc.perform(get("/pedidos/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve criar pedido")
    @WithMockUser(roles = "ADMIN")
    void testCriar() throws Exception {
        when(pedidoService.criar(any(PedidoDTO.class))).thenReturn(pedidoDTO);

        mockMvc.perform(post("/pedidos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pedidoDTO)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve atualizar pedido")
    @WithMockUser(roles = "ADMIN")
    void testAtualizar() throws Exception {
        when(pedidoService.atualizar(eq(1L), any(PedidoDTO.class))).thenReturn(pedidoDTO);

        mockMvc.perform(put("/pedidos/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pedidoDTO)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve listar pedidos com paginação")
    @WithMockUser(roles = "ADMIN")
    void testListarComPaginacao() throws Exception {
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<PedidoDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(pedidoDTO), pageable, 1);
        when(pedidoService.listarComPaginacao(0, 10, "id", "DESC")).thenReturn(page);

        mockMvc.perform(get("/pedidos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Deve rejeitar criação de pedido sem cliente")
    @WithMockUser(roles = "ADMIN")
    void testCriarSemCliente() throws Exception {
        PedidoDTO invalido = PedidoDTO.builder()
            .itens(List.of(ItemPedidoDTO.builder().produtoId(5L).quantidade(1).build()))
            .build();

        mockMvc.perform(post("/pedidos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalido)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar criação de pedido sem produtos")
    @WithMockUser(roles = "ADMIN")
    void testCriarSemProdutos() throws Exception {
        PedidoDTO invalido = PedidoDTO.builder().clienteId(10L).itens(List.of()).build();

        mockMvc.perform(post("/pedidos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalido)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve deletar pedido")
    @WithMockUser(roles = "ADMIN")
    void testDeletar() throws Exception {
        doNothing().when(pedidoService).deletar(1L);

        mockMvc.perform(delete("/pedidos/1").with(csrf()))
            .andExpect(status().isNoContent());
    }
}
