package com.ecommerce.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.api.model.Pagamento;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
}