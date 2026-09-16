package br.com.spbank.pix.adapter.out.persistence.mysql.repository;

import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixKeyData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PixKeyJpaRepository
        extends JpaRepository<PixKeyData, UUID> {

    Optional<PixKeyData>
            findByNormalizedValueAndActiveTrue(
                String normalizedValue
            );

    Optional<PixKeyData>
            findByIdAndAccountId(
                UUID id,
                UUID accountId
            );

    List<PixKeyData>
            findByAccountIdAndActiveTrueOrderByCreatedAtAsc(
                UUID accountId
            );

    long countByAccountIdAndActiveTrue(
        UUID accountId
    );
}