package com.estacionamento_api.estacionamento.repository;

import com.estacionamento_api.estacionamento.model.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    Optional<Entrada> findByVeiculoIdAndAtivoTrue(Long veiculoId);
    
    @Query("SELECT e FROM Entrada e WHERE e.ativo = true")
    List<Entrada> findAllAtivas();
    
    @Query("SELECT COUNT(e) FROM Entrada e WHERE e.ativo = true")
    long countVeiculosEstacionados();
}
      