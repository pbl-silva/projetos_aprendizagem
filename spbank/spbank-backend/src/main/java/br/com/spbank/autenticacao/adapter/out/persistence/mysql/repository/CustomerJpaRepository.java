package br.com.spbank.autenticacao.adapter.out.persistence.mysql.repository;

import br.com.spbank.autenticacao.adapter.out.persistence.mysql.data.CustomerData;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository
        extends JpaRepository<CustomerData, UUID> {

    Optional<CustomerData> findByCpf(
            String cpf
    );
}