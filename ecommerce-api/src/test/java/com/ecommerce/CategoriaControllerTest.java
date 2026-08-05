package com.ecommerce;

import com.ecommerce.api.dto.CategoriaDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.service.CategoriaService;
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
@DisplayName("Testes da API de Categorias")
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoriaService categoriaService;

    private CategoriaDTO categoriaDTO;

    @BeforeEach
    void setUp() {
        categoriaDTO = CategoriaDTO.builder()
            .id(1L)
            .nome("Eletrônicos")
            .descricao("Produtos eletrônicos")
            .ativo(true)
            .build();
    }

    @Test
    @DisplayName("Deve listar categorias")
    @WithMockUser
    void testListar() throws Exception {
        when(categoriaService.listarTodas()).thenReturn(List.of(categoriaDTO));

        mockMvc.perform(get("/categorias/todas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nome", is("Eletrônicos")));
    }

    @Test
    @DisplayName("Deve obter categoria por ID")
    @WithMockUser
    void testObter() throws Exception {
        when(categoriaService.obterPorId(1L)).thenReturn(categoriaDTO);

        mockMvc.perform(get("/categorias/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome", is("Eletrônicos")));
    }

    @Test
    @DisplayName("Deve retornar 404 para categoria inexistente")
    @WithMockUser
    void testObterNaoEncontrada() throws Exception {
        when(categoriaService.obterPorId(999L))
            .thenThrow(new RecursoNaoEncontradoException("Categoria não encontrada"));

        mockMvc.perform(get("/categorias/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve criar categoria")
    @WithMockUser
    void testCriar() throws Exception {
        when(categoriaService.criar(any(CategoriaDTO.class))).thenReturn(categoriaDTO);

        mockMvc.perform(post("/categorias")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoriaDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome", is("Eletrônicos")));
    }

    @Test
    @DisplayName("Deve atualizar categoria")
    @WithMockUser
    void testAtualizar() throws Exception {
        when(categoriaService.atualizar(eq(1L), any(CategoriaDTO.class))).thenReturn(categoriaDTO);

        mockMvc.perform(put("/categorias/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoriaDTO)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve listar categorias com paginação")
    @WithMockUser
    void testListarComPaginacao() throws Exception {
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<CategoriaDTO> page =
            new org.springframework.data.domain.PageImpl<>(List.of(categoriaDTO), pageable, 1);
        when(categoriaService.listarComPaginacao(0, 10, "id", "DESC")).thenReturn(page);

        mockMvc.perform(get("/categorias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Deve rejeitar criação de categoria com nome em branco")
    @WithMockUser
    void testCriarNomeInvalido() throws Exception {
        CategoriaDTO invalida = CategoriaDTO.builder().nome("").build();

        mockMvc.perform(post("/categorias")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalida)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve deletar categoria")
    @WithMockUser
    void testDeletar() throws Exception {
        doNothing().when(categoriaService).deletar(1L);

        mockMvc.perform(delete("/categorias/1").with(csrf()))
            .andExpect(status().isNoContent());
    }
}