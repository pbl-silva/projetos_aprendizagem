package br.com.spbank.transferencia.adapter.out.persistence.mysql.repository;

import br.com.spbank.transferencia.adapter.out.persistence.mysql.data.TransferData;
import br.com.spbank.transferencia.application.model.TransferStatus;
import br.com.spbank.transferencia.application.model.TransferType;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferJpaRepository
        extends JpaRepository<TransferData, UUID> {

    Optional<TransferData>
            findBySourceAccountIdAndIdempotencyKey(
                    UUID source,
                    UUID key
            );

    List<TransferData>
            findBySourceAccountIdAndStatusOrderByScheduledForAscRequestedAtAsc(
                    UUID source,
                    TransferStatus status,
                    Pageable pageable
            );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM TransferData t
            WHERE t.id = :id
            """)
    Optional<TransferData> findForUpdate(
            @Param("id") UUID id
    );

    @Query("""
            SELECT t.id
            FROM TransferData t
            WHERE t.status = :status
              AND t.scheduledFor <= :today
            ORDER BY t.scheduledFor, t.requestedAt
            """)
    List<UUID> findDueIds(
            @Param("status") TransferStatus status,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(t)
            FROM TransferData t
            WHERE t.sourceAccountId = :sourceId
              AND t.type = :type
              AND t.status = :status
              AND t.processedAt >= :start
              AND t.processedAt < :end
            """)
    long countCompletedBySourceAndTypeBetween(
            @Param("sourceId") UUID sourceId,
            @Param("type") TransferType type,
            @Param("status") TransferStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}