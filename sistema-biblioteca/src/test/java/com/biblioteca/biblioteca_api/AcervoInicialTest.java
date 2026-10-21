package com.biblioteca.biblioteca_api;

import com.biblioteca.biblioteca_api.repositories.ImportacaoAcervoRepository;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import com.biblioteca.biblioteca_api.services.AcervoInicial;
import com.biblioteca.biblioteca_api.services.LeitoresIniciais;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.sql.init.mode=never", "spring.datasource.url=jdbc:h2:mem:acervo-test"})
@Transactional
class AcervoInicialTest {
    @Autowired LivroRepository livros;
    @Autowired ImportacaoAcervoRepository importacoes;
    @Autowired AcervoInicial acervo;
    @Autowired LeitoresIniciais leitoresIniciais;
    @Autowired UsuarioRepository usuarios;

    @Test
    void incluiLeitoresUmaVezEPreservaAlteracoes() {
        assertEquals(3, usuarios.count());
        var leitor = usuarios.findByCpf("12345678901").orElseThrow();
        leitor.setNome("Nome atualizado");
        usuarios.save(leitor);
        importacoes.deleteById("leitores-v1");
        leitoresIniciais.run(new DefaultApplicationArguments());
        assertEquals(3, usuarios.count());
        assertEquals("Nome atualizado", usuarios.findByCpf(leitor.getCpf()).orElseThrow().getNome());
        usuarios.delete(leitor);
        leitoresIniciais.run(new DefaultApplicationArguments());
        assertEquals(2, usuarios.count());
    }

    @Test
    void incluiCincoLivrosDisponiveisSemRecriarExcluidos() {
        assertEquals(5, livros.count());
        assertEquals(5, livros.findByDisponivel(true).size());
        var livro = livros.findByIsbn("9780132350884").orElseThrow();
        livros.delete(livro);
        acervo.run(new DefaultApplicationArguments());
        assertEquals(4, livros.count());
        assertTrue(livros.findByIsbn(livro.getIsbn()).isEmpty());
    }

    @Test
    void preservaDadosExistentesAoImportar() {
        var livro = livros.findByIsbn("9780132350884").orElseThrow();
        livro.setTitulo("Título personalizado");
        livro.setDisponivel(false);
        livros.save(livro);
        importacoes.deleteAll();
        acervo.run(new DefaultApplicationArguments());
        assertEquals(5, livros.count());
        var preservado = livros.findByIsbn(livro.getIsbn()).orElseThrow();
        assertEquals("Título personalizado", preservado.getTitulo());
        assertFalse(preservado.getDisponivel());
    }
}
