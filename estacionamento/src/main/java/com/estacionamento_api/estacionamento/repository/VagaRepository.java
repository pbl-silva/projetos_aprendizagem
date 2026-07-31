package com.estacionamento_api.estacionamento.repository;

import com.estacionamento_api.estacionamento.enums.TipoVaga;
import com.estacionamento_api.estacionamento.model.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VagaRepository extends JpaRepository<Vaga, Long> {
    Optional<Vaga> findByNumero(String numero);
    List<Vaga> findByTipoVagaAndDisponivel(TipoVaga tipoVaga, Boolean disponivel);
    
    @Query("SELECT COUNT(v) FROM Vaga v WHERE v.disponivel = true")
    long countVagasDisponiveis();
    
    @Query("SELECT COUNT(v) FROM Vaga v WHERE v.tipoVaga = :tipo AND v.disponivel = true")
    long countVagasDisponiveisPorTipo(TipoVaga tipo);
}