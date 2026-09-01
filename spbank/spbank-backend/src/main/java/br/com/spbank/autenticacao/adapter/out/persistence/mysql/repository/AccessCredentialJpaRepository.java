package br.com.spbank.autenticacao.adapter.out.persistence.mysql.repository;

import br.com.spbank.autenticacao.adapter.out.persistence.mysql.data.AccessCredentialData;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessCredentialJpaRepository
        extends JpaRepository<AccessCredentialData, UUID> {

    Optional<AccessCredentialData> findByUsernameIgnoreCase(
            String username
    );

    Optional<AccessCredentialData> findByCustomerId(
            UUID customerId
    );

    boolean existsByUsernameIgnoreCase(
            String username
    );
}