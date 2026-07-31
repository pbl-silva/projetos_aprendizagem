package com.estacionamento_api.estacionamento.repository;

import com.estacionamento_api.estacionamento.model.Saida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Repository
public interface SaidaRepository extends JpaRepository<Saida, Long> {
    Page<Saida> findByDataHoraSaidaBetween(LocalDateTime inicio, LocalDateTime fim, Pageable pageable);
    
    @Query("SELECT SUM(s.valorPago) FROM Saida s WHERE s.dataCriacao BETWEEN :inicio AND :fim")
    BigDecimal calcularFaturamento(@Param("inicio") LocalDateTime inicio, 
                                    @Param("fim") LocalDateTime fim);
}