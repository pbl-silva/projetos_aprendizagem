package br.com.spbank.pix.adapter.out.persistence.mysql.repository;

import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixPaymentData;
import br.com.spbank.pix.application.model.PixStatus;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PixPaymentJpaRepository
        extends JpaRepository<PixPaymentData, UUID> {

    Optional<PixPaymentData>
            findByOriginAccountIdAndIdempotencyKey(
                UUID originAccountId,
                UUID idempotencyKey
            );

    List<PixPaymentData>
            findByOriginAccountIdAndStatusOrderByScheduledForAscRequestedAtAsc(
                UUID originAccountId,
                PixStatus status,
                Pageable pageable
            );

    List<PixPaymentData>
            findByOriginAccountIdAndRecurrenceIdAndStatusOrderByOccurrenceNumberAsc(
                UUID originAccountId,
                UUID recurrenceId,
                PixStatus status
            );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM PixPaymentData p
        WHERE p.id = :id
        """)
    Optional<PixPaymentData> findForUpdate(
        @Param("id") UUID id
    );

    @Query("""
        SELECT p.id
        FROM PixPaymentData p
        WHERE p.status = :status
          AND p.scheduledFor <= :today
        ORDER BY p.scheduledFor, p.requestedAt
        """)
    List<UUID> findDueIds(
        @Param("status") PixStatus status,
        @Param("today") LocalDate today,
        Pageable pageable
    );
}