package br.com.spbank.autenticacao.adapter.out.persistence.mysql.repository;

import br.com.spbank.autenticacao.adapter.out.persistence.mysql.data.CustomerData;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerJpaRepository
        extends JpaRepository<CustomerData, UUID> {

    Optional<CustomerData> findByCpf(String cpf);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CustomerData c WHERE c.id = :id")
    Optional<CustomerData> findForUpdate(@Param("id") UUID id);
}