package com.biblioteca.biblioteca_api;

import static org.junit.jupiter.api.Assertions.*;

import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class LivroRepositoryTest {

    @Autowired
    private LivroRepository livroRepository;

    @Test
    @DisplayName("Deve buscar livros por categoria")
    void deveBuscarPorCategoria() {
        Livro l1 = Livro.builder()
                .titulo("T1-" + System.nanoTime())
                .isbn(randomIsbn13())
                .autor("A")
                .categoria(CategoriaLivro.TECNICO)
                .disponivel(true)
                .build();
        Livro l2 = Livro.builder()
                .titulo("T2-" + System.nanoTime())
                .isbn(randomIsbn13())
                .autor("B")
                .categoria(CategoriaLivro.FICCAO)
                .disponivel(true)
                .build();

        livroRepository.save(l1);
        livroRepository.save(l2);

        List<Livro> encontrados = livroRepository.findByCategoria(CategoriaLivro.TECNICO);
        assertFalse(encontrados.isEmpty());
        assertTrue(encontrados.stream().anyMatch(l -> l.getTitulo().equals(l1.getTitulo()) &&
                l.getCategoria() == CategoriaLivro.TECNICO));
    }

    @Test
    @DisplayName("Deve buscar livro por ISBN")
    void deveBuscarPorIsbn() {
        String isbn = randomIsbn13();
        String titulo = "IsbnBook-" + System.nanoTime();
        Livro l = Livro.builder()
                .titulo(titulo)
                .isbn(isbn)
                .autor("Autor")
                .categoria(CategoriaLivro.TECNICO)
                .disponivel(true)
                .build();
        livroRepository.save(l);

        Optional<Livro> opt = livroRepository.findByIsbn(isbn);
        assertTrue(opt.isPresent());
        assertEquals(titulo, opt.get().getTitulo());
    }

    @Test
    @DisplayName("Deve buscar livros por disponibilidade")
    void deveBuscarPorDisponibilidade() {
        String tDisponivel = "Disponivel-" + System.nanoTime();
        String tIndisponivel = "Indisponivel-" + System.nanoTime();

        Livro disponivel = Livro.builder()
                .titulo(tDisponivel)
                .isbn(randomIsbn13())
                .autor("X")
                .categoria(CategoriaLivro.TECNICO)
                .disponivel(true)
                .build();
        Livro indisponivel = Livro.builder()
                .titulo(tIndisponivel)
                .isbn(randomIsbn13())
                .autor("Y")
                .categoria(CategoriaLivro.TECNICO)
                .disponivel(false)
                .build();

        livroRepository.save(disponivel);
        livroRepository.save(indisponivel);

        List<Livro> listaDisponiveis = livroRepository.findByDisponivel(true);
        assertTrue(listaDisponiveis.stream().anyMatch(l -> l.getTitulo().equals(tDisponivel)));

        List<Livro> listaIndisponiveis = livroRepository.findByDisponivel(false);
        assertTrue(listaIndisponiveis.stream().anyMatch(l -> l.getTitulo().equals(tIndisponivel)));
    }

    private String randomIsbn13() {
        // Gera ISBN-13 válido começando com 978 (apto ao regex da sua entidade)
        int[] digits = new int[13];
        digits[0] = 9;
        digits[1] = 7;
        digits[2] = 8;
        for (int i = 3; i < 12; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(0, 10);
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += digits[i] * ((i % 2 == 0) ? 1 : 3);
        }
        int mod = sum % 10;
        digits[12] = (mod == 0) ? 0 : 10 - mod;
        StringBuilder sb = new StringBuilder();
        for (int d : digits) sb.append(d);
        return sb.toString();
    }
}