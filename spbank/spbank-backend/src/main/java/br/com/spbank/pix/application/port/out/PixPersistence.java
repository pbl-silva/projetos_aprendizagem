package br.com.spbank.pix.application.port.out;

import br.com.spbank.pix.application.model.PixFavorite;
import br.com.spbank.pix.application.model.PixKey;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixPayment;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixPersistence {

    Optional<PixKey> findActiveKey(
        PixKeyType type,
        String normalizedValue
    );

    Optional<PixKey> findKey(
        UUID keyId,
        UUID accountId
    );

    long countActiveKeys(
        UUID accountId
    );

    List<PixKey> findActiveKeys(
        UUID accountId
    );

    PixKey saveKey(
        PixKey key
    );

    Optional<PixPayment> findPayment(
        UUID id
    );

    Optional<PixPayment> findPaymentForUpdate(
        UUID id
    );

    Optional<PixPayment> findBySourceAndIdempotencyKey(
        UUID sourceId,
        UUID key
    );

    List<PixPayment> findScheduled(
        UUID sourceId,
        int limit
    );

    List<PixPayment> findScheduledByRecurrence(
        UUID sourceId,
        UUID recurrenceId
    );

    List<UUID> findDueIds(
        LocalDate today,
        int limit
    );

    PixPayment savePayment(
        PixPayment payment
    );

    List<PixPayment> savePayments(
        Collection<PixPayment> payments
    );

    Optional<PixFavorite> findFavorite(
        UUID sourceId,
        PixKeyType type,
        String normalizedKey
    );

    Optional<PixFavorite> findUsableFavorite(
        UUID sourceId,
        PixKeyType type,
        String normalizedKey
    );

    Optional<PixFavorite> findFavoriteById(
        UUID favoriteId,
        UUID sourceId
    );

    List<PixFavorite> findFavorites(
        UUID sourceId
    );

    PixFavorite saveFavorite(
        PixFavorite favorite
    );

    void deleteFavorite(
        PixFavorite favorite
    );

    void deleteFavoritesByPixKeyId(
        UUID pixKeyId
    );
}