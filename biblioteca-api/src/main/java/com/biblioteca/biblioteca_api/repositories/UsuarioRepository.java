package com.biblioteca.biblioteca_api.repositories;

import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);

    List<Usuario> findByTipoUsuario(TipoUsuario tipo);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.dataCadastro > :data""")
    List<Usuario> buscarCadastradosApos(@Param("data") LocalDate data);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}