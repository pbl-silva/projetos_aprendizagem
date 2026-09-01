package br.com.spbank.autenticacao.adapter.out.persistence.mysql.repository;

import br.com.spbank.autenticacao.adapter.out.persistence.mysql.data.AccessSessionData;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessSessionJpaRepository
        extends JpaRepository<AccessSessionData, UUID> {

    Optional<AccessSessionData> findByTokenHash(
            String tokenHash
    );

    @Modifying
    @Query("""
            UPDATE AccessSessionData s
            SET s.revokedAt = :at
            WHERE s.tokenHash = :hash
              AND s.revokedAt IS NULL
            """)
    int revoke(
            @Param("hash")
            String tokenHash,

            @Param("at")
            Instant revokedAt
    );

    @Modifying
    @Query("""
            UPDATE AccessSessionData s
            SET s.accountId = :accountId
            WHERE s.tokenHash = :hash
              AND s.customerId = :customerId
              AND s.revokedAt IS NULL
            """)
    int selectAccount(
            @Param("hash")
            String tokenHash,

            @Param("customerId")
            UUID customerId,

            @Param("accountId")
            UUID accountId
    );
}