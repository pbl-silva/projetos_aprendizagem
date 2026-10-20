package com.biblioteca.biblioteca_api.repositories;

import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    List<Livro> findByCategoria(CategoriaLivro categoria);

    List<Livro> findByDisponivel(Boolean disponivel);

    Optional<Livro> findByIsbn(String isbn);

}
