package com.estacionamento_api.estacionamento.repository;

import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    Optional<Veiculo> findByPlaca(String placa);
    List<Veiculo> findByTipoVeiculo(TipoVeiculo tipoVeiculo);
    boolean existsByPlaca(String placa);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Veiculo v WHERE v.id = :id")
    Optional<Veiculo> findByIdForUpdate(@Param("id") Long id);
}
