package com.biblioteca.biblioteca_api.services;

import com.biblioteca.biblioteca_api.entities.ImportacaoAcervo;
import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import com.biblioteca.biblioteca_api.repositories.ImportacaoAcervoRepository;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AcervoInicial implements ApplicationRunner {
    private final LivroRepository livros;
    private final ImportacaoAcervoRepository importacoes;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (importacoes.existsById("inicial-v1")) return;
        incluir("Dom Casmurro", "9788535902778", "Machado de Assis", 1899, CategoriaLivro.ROMANCE);
        incluir("1984", "9788535914849", "George Orwell", 1949, CategoriaLivro.FICCAO);
        incluir("Sapiens: Uma Breve História da Humanidade", "9788525432186", "Yuval Noah Harari", 2011, CategoriaLivro.NAO_FICCAO);
        incluir("Clean Code", "9780132350884", "Robert C. Martin", 2008, CategoriaLivro.TECNICO);
        incluir("O Silêncio dos Inocentes", "9788501061233", "Thomas Harris", 1988, CategoriaLivro.SUSPENSE);
        importacoes.save(new ImportacaoAcervo("inicial-v1"));
    }

    private void incluir(String titulo, String isbn, String autor, int ano, CategoriaLivro categoria) {
        if (livros.findByIsbn(isbn).isPresent()) return;
        livros.save(Livro.builder().titulo(titulo).isbn(isbn).autor(autor)
            .anoPublicacao(ano).categoria(categoria).disponivel(true).build());
    }
}
