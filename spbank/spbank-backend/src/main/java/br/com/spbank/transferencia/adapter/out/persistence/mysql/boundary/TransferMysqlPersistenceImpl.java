package br.com.spbank.transferencia.adapter.out.persistence.mysql.boundary;

import br.com.spbank.transferencia.adapter.out.persistence.mysql.mapper.TransferPersistenceMapper;
import br.com.spbank.transferencia.adapter.out.persistence.mysql.repository.TransferJpaRepository;
import br.com.spbank.transferencia.application.model.transfer.Transfer;
import br.com.spbank.transferencia.application.model.transfer.TransferStatus;
import br.com.spbank.transferencia.application.model.transfer.TransferType;
import br.com.spbank.transferencia.application.port.out.TransferPersistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public final class TransferMysqlPersistenceImpl
        implements TransferPersistence {

    private final TransferJpaRepository repository;

    public TransferMysqlPersistenceImpl(
            TransferJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<Transfer> findById(UUID id) {

        return repository
                .findById(id)
                .map(TransferPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Transfer> findForUpdate(UUID id) {

        return repository
                .findForUpdate(id)
                .map(TransferPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Transfer> findBySourceAndIdempotencyKey(
            UUID source,
            UUID key
    ) {

        return repository
                .findBySourceAccountIdAndIdempotencyKey(
                        source,
                        key
                )
                .map(TransferPersistenceMapper::toDomain);
    }

    @Override
    public List<Transfer> findBySourceAndStatus(
            UUID source,
            TransferStatus status,
            int limit
    ) {

        return repository
                .findBySourceAccountIdAndStatusOrderByScheduledForAscRequestedAtAsc(
                        source,
                        status,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(TransferPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findDueIds(
            TransferStatus status,
            LocalDate today,
            int limit
    ) {

        return repository.findDueIds(
                status,
                today,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public long countCompletedBySourceAndTypeBetween(
            UUID sourceId,
            TransferType type,
            Instant start,
            Instant end
    ) {

        return repository
                .countCompletedBySourceAndTypeBetween(
                        sourceId,
                        type,
                        TransferStatus.COMPLETED,
                        start,
                        end
                );
    }

    @Override
    public Transfer save(
            Transfer transfer
    ) {

        return TransferPersistenceMapper.toDomain(
                repository.save(
                        TransferPersistenceMapper.toData(
                                transfer
                        )
                )
        );
    }
}