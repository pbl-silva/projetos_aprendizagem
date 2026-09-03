package br.com.spbank.administracao.adapter.out.persistence.mysql.repository;

import br.com.spbank.administracao.adapter.out.persistence.mysql.data.AdministratorSessionData;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdministratorSessionJpaRepository
        extends JpaRepository<AdministratorSessionData, UUID> {

    Optional<AdministratorSessionData> findByTokenHash(
            String tokenHash
    );

    @Modifying
    @Query("""
            UPDATE AdministratorSessionData s
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
}