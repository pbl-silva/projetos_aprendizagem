package com.ecommerce;

import com.ecommerce.api.dto.ProdutoDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.service.ProdutoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes da API de Produtos")
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProdutoService produtoService;

    private ProdutoDTO produtoDTO;

    @BeforeEach
    void setUp() {
        // O service sempre trabalha com ProdutoDTO (nao com a entidade Produto),
        // entao o mock tambem deve usar ProdutoDTO.
        produtoDTO = ProdutoDTO.builder()
            .id(1L)
            .nome("Notebook Dell")
            .descricao("Notebook 15 polegadas")
            .preco(new BigDecimal("3500.00"))
            .estoque(10)
            .categoriaId(1L)
            .ativo(true)
            .build();
    }

    @Test
    @DisplayName("Deve retornar todos os produtos")
    @WithMockUser(roles = "ADMIN")
    void testListarTodos() throws Exception {
        when(produtoService.listarTodos()).thenReturn(List.of(produtoDTO));

        mockMvc.perform(get("/produtos/todos")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nome", is("Notebook Dell")))
            .andExpect(jsonPath("$[0].preco", is(3500.00)));

        verify(produtoService, times(1)).listarTodos();
    }

    @Test
    @DisplayName("Deve retornar produto por ID")
    @WithMockUser(roles = "ADMIN")
    void testObterPorId() throws Exception {
        // obterPorId retorna ProdutoDTO diretamente (lanca excecao se nao encontrar,
        // nao retorna Optional)
        when(produtoService.obterPorId(1L)).thenReturn(produtoDTO);

        mockMvc.perform(get("/produtos/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.nome", is("Notebook Dell")))
            .andExpect(jsonPath("$.preco", is(3500.00)));

        verify(produtoService, times(1)).obterPorId(1L);
    }

    @Test
    @DisplayName("Deve retornar 404 quando produto nao existe")
    @WithMockUser(roles = "ADMIN")
    void testObterPorIdNaoEncontrado() throws Exception {
        when(produtoService.obterPorId(999L))
            .thenThrow(new RecursoNaoEncontradoException("Produto nao encontrado com ID: 999"));

        mockMvc.perform(get("/produtos/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(produtoService, times(1)).obterPorId(999L);
    }

    @Test
    @DisplayName("Deve criar novo produto")
    @WithMockUser(roles = "ADMIN")
    void testCriar() throws Exception {
        when(produtoService.criar(any(ProdutoDTO.class))).thenReturn(produtoDTO);

        mockMvc.perform(post("/produtos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produtoDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.nome", is("Notebook Dell")));

        verify(produtoService, times(1)).criar(any(ProdutoDTO.class));
    }

    @Test
    @DisplayName("Deve atualizar produto existente")
    @WithMockUser(roles = "ADMIN")
    void testAtualizar() throws Exception {
        when(produtoService.atualizar(eq(1L), any(ProdutoDTO.class))).thenReturn(produtoDTO);

        mockMvc.perform(put("/produtos/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produtoDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome", is("Notebook Dell")));

        verify(produtoService, times(1)).atualizar(eq(1L), any(ProdutoDTO.class));
    }

    @Test
    @DisplayName("Deve deletar produto")
    @WithMockUser(roles = "ADMIN")
    void testDeletar() throws Exception {
        doNothing().when(produtoService).deletar(1L);

        mockMvc.perform(delete("/produtos/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(produtoService, times(1)).deletar(1L);
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticacao")
    void testSemAutenticacao() throws Exception {
        mockMvc.perform(get("/produtos")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve diminuir estoque")
    @WithMockUser(roles = "ADMIN")
    void testDiminuirEstoque() throws Exception {
        when(produtoService.diminuirEstoque(1L, 5)).thenReturn(5);

        mockMvc.perform(patch("/produtos/1/estoque/diminuir")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .param("quantidade", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(5)));

        verify(produtoService, times(1)).diminuirEstoque(1L, 5);
    }

    @Test
    @DisplayName("Deve aumentar estoque")
    @WithMockUser(roles = "ADMIN")
    void testAumentarEstoque() throws Exception {
        when(produtoService.aumentarEstoque(1L, 5)).thenReturn(15);

        mockMvc.perform(patch("/produtos/1/estoque/aumentar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .param("quantidade", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(15)));

        verify(produtoService, times(1)).aumentarEstoque(1L, 5);
    }
}