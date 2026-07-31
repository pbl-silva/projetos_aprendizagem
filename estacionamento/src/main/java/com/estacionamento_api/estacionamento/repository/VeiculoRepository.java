package com.estacionamento_api.estacionamento.repository;

import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    Optional<Veiculo> findByPlaca(String placa);
    List<Veiculo> findByTipoVeiculo(TipoVeiculo tipoVeiculo);
    boolean existsByPlaca(String placa);
}