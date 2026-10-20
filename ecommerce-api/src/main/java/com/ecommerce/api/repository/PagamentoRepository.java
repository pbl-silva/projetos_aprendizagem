package com.ecommerce.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.api.model.Pagamento;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByPedidoClienteEmailOrderByDataCriacaoDesc(String email);
    List<Pagamento> findByPedidoIdOrderByDataCriacaoDesc(Long pedidoId);
}
