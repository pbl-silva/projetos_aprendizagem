package com.ecommerce;

import com.ecommerce.api.dto.CategoriaDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Categoria;
import com.ecommerce.api.repository.CategoriaRepository;
import com.ecommerce.api.service.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do CategoriaService")
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;
    private CategoriaDTO categoriaDTO;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder()
            .id(1L)
            .nome("Eletrônicos")
            .descricao("Produtos eletrônicos em geral")
            .ativo(true)
            .build();

        categoriaDTO = CategoriaDTO.builder()
            .id(1L)
            .nome("Eletrônicos")
            .descricao("Produtos eletrônicos em geral")
            .ativo(true)
            .build();
    }

    @Test
    @DisplayName("Deve criar categoria")
    void testCriar() {
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        CategoriaDTO resultado = categoriaService.criar(categoriaDTO);

        assertNotNull(resultado);
        assertEquals("Eletrônicos", resultado.getNome());
        verify(categoriaRepository, times(1)).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Deve obter categoria por ID")
    void testObterPorId() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        CategoriaDTO resultado = categoriaService.obterPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    @DisplayName("Deve lançar exceção ao obter categoria inexistente")
    void testObterPorIdNaoEncontrada() {
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
            () -> categoriaService.obterPorId(999L));
    }

    @Test
    @DisplayName("Deve listar todas as categorias")
    void testListarTodas() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria));

        List<CategoriaDTO> resultado = categoriaService.listarTodas();

        assertEquals(1, resultado.size());
        assertEquals("Eletrônicos", resultado.get(0).getNome());
    }

    @Test
    @DisplayName("Deve atualizar categoria existente")
    void testAtualizar() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        CategoriaDTO atualizacao = CategoriaDTO.builder()
            .nome("Eletrônicos Atualizados")
            .descricao("Nova descrição")
            .build();

        CategoriaDTO resultado = categoriaService.atualizar(1L, atualizacao);

        assertNotNull(resultado);
        verify(categoriaRepository, times(1)).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar categoria inexistente")
    void testAtualizarNaoEncontrada() {
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
            () -> categoriaService.atualizar(999L, categoriaDTO));
    }

    @Test
    @DisplayName("Deve listar categorias com paginação")
    void testListarComPaginacao() {
        org.springframework.data.domain.Page<Categoria> page =
            new org.springframework.data.domain.PageImpl<>(List.of(categoria));
        when(categoriaRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        var resultado = categoriaService.listarComPaginacao(0, 10, "id", "ASC");

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Deve deletar categoria existente")
    void testDeletar() {
        when(categoriaRepository.existsById(1L)).thenReturn(true);

        categoriaService.deletar(1L);

        verify(categoriaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar categoria inexistente")
    void testDeletarNaoEncontrada() {
        when(categoriaRepository.existsById(999L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class,
            () -> categoriaService.deletar(999L));

        verify(categoriaRepository, never()).deleteById(any());
    }
}