package br.com.spbank.administracao.adapter.out.persistence.mysql.repository;

import br.com.spbank.administracao.adapter.out.persistence.mysql.data.AdministratorCredentialData;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorCredentialJpaRepository
        extends JpaRepository<AdministratorCredentialData, UUID> {

    Optional<AdministratorCredentialData> findByUsernameIgnoreCase(
            String username
    );
}