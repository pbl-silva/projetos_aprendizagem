package com.biblioteca.biblioteca_api.repositories;

import com.biblioteca.biblioteca_api.entities.Emprestimo;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    List<Emprestimo> findByUsuarioAndStatus(Usuario usuario, StatusEmprestimo status);

    List<Emprestimo> findByUsuarioId(Long usuarioId);

    List<Emprestimo> findByStatus(StatusEmprestimo status);

    @Query("""
            SELECT e FROM Emprestimo e
            WHERE e.status = 'ATIVO'
            AND e.dataDevolucaoPrevista < :hoje""")
    List<Emprestimo> buscarEmprestimosAtrasados(@Param("hoje") LocalDate hoje);

    @Query("""
            SELECT COUNT(e) FROM Emprestimo e
            WHERE e.usuario = :usuario
            AND e.status = 'ATIVO'""")
    long contarEmprestimosAtivosPorUsuario(@Param("usuario") Usuario usuario);

}
