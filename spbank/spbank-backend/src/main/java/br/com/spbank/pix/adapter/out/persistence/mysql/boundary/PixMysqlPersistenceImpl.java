package br.com.spbank.pix.adapter.out.persistence.mysql.boundary;

import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixFavoriteData;
import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixKeyData;
import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixPaymentData;
import br.com.spbank.pix.adapter.out.persistence.mysql.repository.PixFavoriteJpaRepository;
import br.com.spbank.pix.adapter.out.persistence.mysql.repository.PixKeyJpaRepository;
import br.com.spbank.pix.adapter.out.persistence.mysql.repository.PixPaymentJpaRepository;
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
    public Optional<PixKey> findActiveKey(
        PixKeyType type,
        String value
    ) {
        return keys
            .findByTypeAndNormalizedValueAndActiveTrue(
                type,
                value
            )
            .map(
                PixMysqlPersistenceImpl::key
            );
    }

    @Override
    public Optional<PixKey> findKey(
        UUID id,
        UUID account
    ) {
        return keys
            .findByIdAndAccountId(
                id,
                account
            )
            .map(
                PixMysqlPersistenceImpl::key
            );
    }

    @Override
    public long countActiveKeys(
        UUID account
    ) {
        return keys
            .countByAccountIdAndActiveTrue(
                account
            );
    }

    @Override
    public List<PixKey> findActiveKeys(
        UUID account
    ) {
        return keys
            .findByAccountIdAndActiveTrueOrderByCreatedAtAsc(
                account
            )
            .stream()
            .map(
                PixMysqlPersistenceImpl::key
            )
            .toList();
    }

    @Override
    public PixKey saveKey(
        PixKey value
    ) {
        return key(
            keys.save(
                data(value)
            )
        );
    }

    @Override
    public Optional<PixPayment> findPayment(
        UUID id
    ) {
        return payments
            .findById(id)
            .map(
                PixMysqlPersistenceImpl::payment
            );
    }

    @Override
    public Optional<PixPayment> findPaymentForUpdate(
        UUID id
    ) {
        return payments
            .findForUpdate(id)
            .map(
                PixMysqlPersistenceImpl::payment
            );
    }

    @Override
    public Optional<PixPayment> findBySourceAndIdempotencyKey(
        UUID source,
        UUID key
    ) {
        return payments
            .findBySourceAccountIdAndIdempotencyKey(
                source,
                key
            )
            .map(
                PixMysqlPersistenceImpl::payment
            );
    }

    @Override
    public List<PixPayment> findScheduled(
        UUID source,
        int limit
    ) {
        return payments
            .findBySourceAccountIdAndStatusOrderByScheduledForAscRequestedAtAsc(
                source,
                PixStatus.SCHEDULED,
                PageRequest.of(
                    0,
                    limit
                )
            )
            .stream()
            .map(
                PixMysqlPersistenceImpl::payment
            )
            .toList();
    }

    @Override
    public List<PixPayment> findScheduledByRecurrence(
        UUID source,
        UUID recurrence
    ) {
        return payments
            .findBySourceAccountIdAndRecurrenceIdAndStatusOrderByScheduledForAsc(
                source,
                recurrence,
                PixStatus.SCHEDULED
            )
            .stream()
            .map(
                PixMysqlPersistenceImpl::payment
            )
            .toList();
    }

    @Override
    public List<UUID> findDueIds(
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
        PixPayment value
    ) {
        return payment(
            payments.save(
                data(value)
            )
        );
    }

    @Override
    public List<PixPayment> savePayments(
        Collection<PixPayment> values
    ) {
        return payments
            .saveAll(java.util.Objects.requireNonNull(
                values
                    .stream()
                    .map(
                        PixMysqlPersistenceImpl::data
                    )
                    .toList()
            ))
            .stream()
            .map(
                PixMysqlPersistenceImpl::payment
            )
            .toList();
    }

    @Override
    public Optional<PixFavorite> findFavorite(
        UUID source,
        PixKeyType type,
        String normalizedKey
    ) {
        return favorites
            .findBySourceAccountIdAndKeyTypeAndNormalizedKey(
                source,
                type,
                normalizedKey
            )
            .map(
                PixMysqlPersistenceImpl::favorite
            );
    }

    @Override
    public Optional<PixFavorite> findUsableFavorite(
        UUID source,
        PixKeyType type,
        String normalizedKey
    ) {
        return favorites
            .findUsable(
                source,
                type,
                normalizedKey
            )
            .map(
                PixMysqlPersistenceImpl::favorite
            );
    }

    @Override
    public Optional<PixFavorite> findFavoriteById(
        UUID id,
        UUID source
    ) {
        return favorites
            .findByIdAndSourceAccountId(
                id,
                source
            )
            .map(
                PixMysqlPersistenceImpl::favorite
            );
    }

    @Override
    public List<PixFavorite> findFavorites(
        UUID source
    ) {
        return favorites
            .findActiveBySource(
                source
            )
            .stream()
            .map(
                PixMysqlPersistenceImpl::favorite
            )
            .toList();
    }

    @Override
    public PixFavorite saveFavorite(
        PixFavorite value
    ) {
        return favorite(
            favorites.save(
                data(value)
            )
        );
    }

    @Override
    public void deleteFavorite(
        PixFavorite value
    ) {
        favorites.delete(
            data(value)
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

    private static PixKey key(
        PixKeyData data
    ) {
        return new PixKey(
            data.getId(),
            data.getAccountId(),
            data.getType(),
            data.getNormalizedValue(),
            data.isActive(),
            data.getCreatedAt()
        );
    }

    private static PixKeyData data(
        PixKey key
    ) {
        return new PixKeyData(
            key.getId(),
            key.getAccountId(),
            key.getType(),
            key.getNormalizedValue(),
            key.isActive(),
            key.getCreatedAt()
        );
    }

    private static PixFavorite favorite(
        PixFavoriteData data
    ) {
        return new PixFavorite(
            data.getId(),
            data.getSourceAccountId(),
            data.getPixKeyId(),
            data.getRecipientName(),
            data.getNormalizedKey(),
            data.getMaskedKey(),
            data.getKeyType(),
            data.getRouting(),
            data.getBankCode(),
            data.getBankName(),
            data.getMaskedRecipientDocument(),
            data.getCreatedAt()
        );
    }

    private static PixFavoriteData data(
        PixFavorite favorite
    ) {
        return new PixFavoriteData(
            favorite.id(),
            favorite.sourceAccountId(),
            favorite.pixKeyId(),
            favorite.keyValue(),
            favorite.routing(),
            favorite.recipientName(),
            favorite.bankCode(),
            favorite.bankName(),
            favorite.maskedRecipientDocument(),
            favorite.maskedKey(),
            favorite.keyType(),
            favorite.createdAt()
        );
    }

    private static PixPayment payment(
        PixPaymentData data
    ) {
        return new PixPayment(
            data.getId(),
            data.getSourceAccountId(),
            data.getTargetAccountId(),
            data.getRouting(),
            data.getKeyType(),
            data.getMaskedKey(),
            data.getKeyHash(),
            data.getRecipientName(),
            data.getMaskedRecipientDocument(),
            data.getDestinationBankCode(),
            data.getDestinationBankName(),
            data.getAmount(),
            data.getStatus(),
            data.getIdempotencyKey(),
            data.getRequestFingerprint(),
            data.getRequestedAt(),
            data.getScheduledFor(),
            data.getProcessedAt(),
            data.getSettlementReference(),
            data.getFailureCode(),
            data.getFailureMessageKey(),
            data.getRecurrenceId(),
            data.getFrequency(),
            data.getOccurrenceNumber(),
            data.getTotalOccurrences()
        );
    }

    private static PixPaymentData data(
        PixPayment payment
    ) {
        return new PixPaymentData(
            payment.getId(),
            payment.getSourceAccountId(),
            payment.getTargetAccountId(),
            payment.getRouting(),
            payment.getKeyType(),
            payment.getMaskedKey(),
            payment.getKeyHash(),
            payment.getRecipientName(),
            payment.getMaskedRecipientDocument(),
            payment.getDestinationBankCode(),
            payment.getDestinationBankName(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getIdempotencyKey(),
            payment.getRequestFingerprint(),
            payment.getRequestedAt(),
            payment.getScheduledFor(),
            payment.getProcessedAt(),
            payment.getSettlementReference(),
            payment.getFailureCode(),
            payment.getFailureMessageKey(),
            payment.getRecurrenceId(),
            payment.getFrequency(),
            payment.getOccurrenceNumber(),
            payment.getTotalOccurrences()
        );
    }
}