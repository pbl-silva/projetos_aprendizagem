package com.biblioteca.biblioteca_api.repositories;

import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    List<Livro> findByCategoria(CategoriaLivro categoria);

    List<Livro> findByDisponivel(Boolean disponivel);

    Optional<Livro> findByIsbn(String isbn);

    @Query("""
            SELECT l FROM Livro l
            WHERE LOWER(l.autor)
            LIKE LOWER(CONCAT('%', :autor, '%'))""")
    List<Livro> buscarPorAutor(@Param("autor") String autor);

    @Query("""
            SELECT l.categoria,
            COUNT(l) FROM Livro l
            WHERE l.disponivel = true
            GROUP BY l.categoria""")
    List<Object[]> contarLivrosDisponiveisPorCategoria();
}