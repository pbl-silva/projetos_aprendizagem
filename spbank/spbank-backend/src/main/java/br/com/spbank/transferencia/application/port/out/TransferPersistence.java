package br.com.spbank.transferencia.application.port.out;

import br.com.spbank.transferencia.application.model.transfer.Transfer;
import br.com.spbank.transferencia.application.model.transfer.TransferStatus;
import br.com.spbank.transferencia.application.model.transfer.TransferType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferPersistence {

    Optional<Transfer> findById(UUID id);

    Optional<Transfer> findForUpdate(UUID id);

    Optional<Transfer> findBySourceAndIdempotencyKey(
            UUID sourceId,
            UUID key
    );

    List<Transfer> findBySourceAndStatus(
            UUID sourceId,
            TransferStatus status,
            int limit
    );

    List<UUID> findDueIds(
            TransferStatus status,
            LocalDate today,
            int limit
    );

    long countCompletedBySourceAndTypeBetween(
            UUID sourceId,
            TransferType type,
            Instant start,
            Instant end
    );

    Transfer save(Transfer transfer);
}