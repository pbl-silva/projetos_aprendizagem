package com.biblioteca.biblioteca_api;

import static org.junit.jupiter.api.Assertions.*;

import com.biblioteca.biblioteca_api.entities.Emprestimo;
import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import com.biblioteca.biblioteca_api.repositories.EmprestimoRepository;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class EmprestimoRepositoryTest {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Deve buscar empréstimos ativos por usuário")
    void deveBuscarEmprestimosAtivosPorUsuario() {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Test")
                .isbn(randomIsbn13())
                .autor("Autor")
                .categoria(CategoriaLivro.TECNICO)
                .disponivel(false)
                .build());

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("João")
                .email("joao.ativo+" + UUID.randomUUID().toString() + "@test.com")
                .cpf(randomCpf())
                .tipoUsuario(TipoUsuario.COMUM)
                .dataCadastro(LocalDate.now())
                .build());

        emprestimoRepository.save(Emprestimo.builder()
                .livro(livro)
                .usuario(usuario)
                .dataEmprestimo(LocalDate.now())
                .dataDevolucaoPrevista(LocalDate.now().plusDays(7))
                .status(StatusEmprestimo.ATIVO)
                .build());

        List<Emprestimo> resultado = emprestimoRepository
                .findByUsuarioAndStatus(usuario, StatusEmprestimo.ATIVO);

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        assertEquals(usuario.getId(), resultado.get(0).getUsuario().getId());
    }

    @Test
    @DisplayName("Deve contar empréstimos ativos por usuário")
    void deveContarEmprestimosAtivos() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("Maria")
                .email("maria.count+" + UUID.randomUUID().toString() + "@test.com")
                .cpf(randomCpf())
                .tipoUsuario(TipoUsuario.PREMIUM)
                .dataCadastro(LocalDate.now())
                .build());

        List<Emprestimo> lista = emprestimoRepository.findByUsuarioAndStatus(usuario, StatusEmprestimo.ATIVO);
        Long count = (long) lista.size();

        assertEquals(0L, count);
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

    private String randomCpf() {
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 11; i++) sb.append(r.nextInt(0, 10));
        return sb.toString();
    }
}