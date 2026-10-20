package com.biblioteca.biblioteca_api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.biblioteca.biblioteca_api.dto.request.LivroRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.LivroResponseDTO;
import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import com.biblioteca.biblioteca_api.exceptions.BusinessException;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import com.biblioteca.biblioteca_api.services.impl.LivroServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock
    private LivroRepository livroRepository;

    @InjectMocks
    private LivroServiceImpl livroService;

    @Test
    @DisplayName("Deve salvar livro com ISBN único")
    void deveSalvarLivroComIsbnUnico() {
        LivroRequestDTO request = LivroRequestDTO.builder()
                .titulo("Clean Code")
                .isbn("978-0132350884")
                .autor("Robert C. Martin")
                .anoPublicacao(null)
                .categoria(CategoriaLivro.TECNICO)
                .build();

        when(livroRepository.findByIsbn("978-0132350884")).thenReturn(Optional.empty());
        when(livroRepository.save(ArgumentMatchers.any(Livro.class)))
                .thenAnswer(inv -> {
                    Livro l = inv.getArgument(0);
                    l.setId(1L);
                    return l;
                });

        LivroResponseDTO resp = livroService.criar(request);

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        verify(livroRepository).findByIsbn("978-0132350884");
        verify(livroRepository).save(ArgumentMatchers.any(Livro.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar livro com ISBN duplicado")
    void deveLancarAoSalvarIsbnDuplicado() {
        LivroRequestDTO request = LivroRequestDTO.builder()
                .titulo("Clean Code")
                .isbn("978-0132350884")
                .autor("Robert C. Martin")
                .anoPublicacao(null)
                .categoria(CategoriaLivro.TECNICO)
                .build();

        when(livroRepository.findByIsbn("978-0132350884"))
                .thenReturn(Optional.of(Livro.builder().id(1L).isbn("978-0132350884").build()));

        assertThrows(BusinessException.class, () -> livroService.criar(request));
        verify(livroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar livro por id com sucesso")
    void deveBuscarPorId() {
        Livro livro = Livro.builder().id(1L).titulo("Clean Code").isbn("978-0132350884").build();
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));

        LivroResponseDTO resp = livroService.buscarPorId(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        assertEquals("Clean Code", resp.getTitulo());
    }

    @Test
    @DisplayName("Deve listar todos os livros")
    void deveListarLivros() {
        Livro l1 = Livro.builder().id(1L).titulo("Clean Code").build();
        Livro l2 = Livro.builder().id(2L).titulo("Refactoring").build();
        when(livroRepository.findAll()).thenReturn(List.of(l1, l2));

        List<LivroResponseDTO> lista = livroService.listarTodos();

        assertNotNull(lista);
        assertEquals(2, lista.size());
        verify(livroRepository).findAll();
    }
}