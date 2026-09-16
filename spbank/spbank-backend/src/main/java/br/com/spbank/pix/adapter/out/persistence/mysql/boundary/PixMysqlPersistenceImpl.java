package br.com.spbank.pix.adapter.out.persistence.mysql.boundary;

import br.com.spbank.pix.adapter.out.persistence.mysql.mapper.PixPersistenceMapper;
import br.com.spbank.pix.adapter.out.persistence.mysql.repository.PixFavoriteJpaRepository;
import br.com.spbank.pix.adapter.out.persistence.mysql.repository.PixKeyJpaRepository;
import br.com.spbank.pix.adapter.out.persistence.mysql.repository.PixPaymentJpaRepository;
import br.com.spbank.pix.application.model.PixDestinationScope;
import br.com.spbank.pix.application.model.PixFavorite;
import br.com.spbank.pix.application.model.PixKey;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixPayment;
import br.com.spbank.pix.application.model.PixStatus;
import br.com.spbank.pix.application.port.out.PixPersistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public final class PixMysqlPersistenceImpl
        implements PixPersistence {

    private final PixKeyJpaRepository keys;
    private final PixPaymentJpaRepository payments;
    private final PixFavoriteJpaRepository favorites;

    public PixMysqlPersistenceImpl(
        PixKeyJpaRepository keys,
        PixPaymentJpaRepository payments,
        PixFavoriteJpaRepository favorites
    ) {
        this.keys = keys;
        this.payments = payments;
        this.favorites = favorites;
    }

    @Override
    public Optional<PixKey> findActiveKeyByNormalizedValue(
        String normalizedValue
    ) {
        return keys
            .findByNormalizedValueAndActiveTrue(
                normalizedValue
            )
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public Optional<PixKey> findKeyByIdAndAccountId(
        UUID keyId,
        UUID accountId
    ) {
        return keys
            .findByIdAndAccountId(
                keyId,
                accountId
            )
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public List<PixKey> findActiveKeysByAccountId(
        UUID accountId
    ) {
        return keys
            .findByAccountIdAndActiveTrueOrderByCreatedAtAsc(
                accountId
            )
            .stream()
            .map(
                PixPersistenceMapper::toDomain
            )
            .toList();
    }

    @Override
    public long countActiveKeysByAccountId(
        UUID accountId
    ) {
        return keys
            .countByAccountIdAndActiveTrue(
                accountId
            );
    }

    @Override
    public PixKey saveKey(
        PixKey key
    ) {
        return PixPersistenceMapper.toDomain(
            keys.save(
                PixPersistenceMapper.toData(
                    key
                )
            )
        );
    }

    @Override
    public Optional<PixPayment> findPaymentById(
        UUID paymentId
    ) {
        return payments
            .findById(paymentId)
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public Optional<PixPayment> findPaymentForUpdate(
        UUID paymentId
    ) {
        return payments
            .findForUpdate(paymentId)
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public Optional<PixPayment>
            findPaymentBySourceAndIdempotencyKey(
                UUID sourceAccountId,
                UUID idempotencyKey
            ) {

        return payments
            .findByOriginAccountIdAndIdempotencyKey(
                sourceAccountId,
                idempotencyKey
            )
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public List<PixPayment>
            findScheduledPaymentsBySource(
                UUID sourceAccountId,
                int limit
            ) {

        return payments
            .findByOriginAccountIdAndStatusOrderByScheduledForAscRequestedAtAsc(
                sourceAccountId,
                PixStatus.SCHEDULED,
                PageRequest.of(
                    0,
                    limit
                )
            )
            .stream()
            .map(
                PixPersistenceMapper::toDomain
            )
            .toList();
    }

    @Override
    public List<PixPayment>
            findScheduledPaymentsByRecurrence(
                UUID sourceAccountId,
                UUID recurrenceId
            ) {

        return payments
            .findByOriginAccountIdAndRecurrenceIdAndStatusOrderByOccurrenceNumberAsc(
                sourceAccountId,
                recurrenceId,
                PixStatus.SCHEDULED
            )
            .stream()
            .map(
                PixPersistenceMapper::toDomain
            )
            .toList();
    }

    @Override
    public List<UUID> findDuePaymentIds(
        LocalDate today,
        int limit
    ) {
        return payments.findDueIds(
            PixStatus.SCHEDULED,
            today,
            PageRequest.of(
                0,
                limit
            )
        );
    }

    @Override
    public PixPayment savePayment(
        PixPayment payment
    ) {
        return PixPersistenceMapper.toDomain(
            payments.save(
                PixPersistenceMapper.toData(
                    payment
                )
            )
        );
    }

    @Override
    public List<PixPayment> savePayments(
        Collection<PixPayment> pixPayments
    ) {
        return payments
            .saveAll(
                pixPayments
                    .stream()
                    .map(
                        PixPersistenceMapper::toData
                    )
                    .toList()
            )
            .stream()
            .map(
                PixPersistenceMapper::toDomain
            )
            .toList();
    }

    @Override
    public List<PixFavorite> findFavoritesBySource(
        UUID sourceAccountId
    ) {
        return favorites
            .findByOriginAccountIdOrderByCreatedAtAsc(
                sourceAccountId
            )
            .stream()
            .map(
                PixPersistenceMapper::toDomain
            )
            .toList();
    }

    @Override
    public Optional<PixFavorite>
            findFavoriteByIdAndSource(
                UUID favoriteId,
                UUID sourceAccountId
            ) {

        return favorites
            .findByIdAndOriginAccountId(
                favoriteId,
                sourceAccountId
            )
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public Optional<PixFavorite>
            findFavoriteBySourceAndKey(
                UUID sourceAccountId,
                PixKeyType keyType,
                String normalizedKey
            ) {

        return favorites
            .findByOriginAccountIdAndKeyTypeAndNormalizedKeyValue(
                sourceAccountId,
                keyType,
                normalizedKey
            )
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public Optional<PixFavorite> findUsableFavorite(
        UUID sourceAccountId,
        PixKeyType keyType,
        String normalizedKey
    ) {
        return favorites
            .findUsableFavorite(
                sourceAccountId,
                keyType,
                normalizedKey,
                PixDestinationScope.INTERNAL,
                PixDestinationScope.EXTERNAL
            )
            .map(
                PixPersistenceMapper::toDomain
            );
    }

    @Override
    public PixFavorite saveFavorite(
        PixFavorite favorite
    ) {
        return PixPersistenceMapper.toDomain(
            favorites.save(
                PixPersistenceMapper.toData(
                    favorite
                )
            )
        );
    }

    @Override
    public void deleteFavorite(
        UUID favoriteId,
        UUID sourceAccountId
    ) {
        favorites.deleteByIdAndOriginAccountId(
            favoriteId,
            sourceAccountId
        );
    }

    @Override
    public void deleteFavoritesByPixKeyId(
        UUID pixKeyId
    ) {
        favorites.deleteByPixKeyId(
            pixKeyId
        );
    }
}