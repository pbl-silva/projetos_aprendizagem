package com.estacionamento_api.estacionamento.repository;

import com.estacionamento_api.estacionamento.enums.TipoVaga;
import com.estacionamento_api.estacionamento.model.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface VagaRepository extends JpaRepository<Vaga, Long> {
    boolean existsByNumero(String numero);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Vaga> findFirstByTipoVagaAndDisponivelTrueOrderByIdAsc(TipoVaga tipoVaga);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vaga v WHERE v.id = :id")
    Optional<Vaga> findByIdForUpdate(@Param("id") Long id);
    
    @Query("SELECT COUNT(v) FROM Vaga v WHERE v.disponivel = true")
    long countVagasDisponiveis();
    
}
