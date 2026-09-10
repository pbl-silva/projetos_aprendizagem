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

    Optional<PixKey> findActiveKeyByNormalizedValue(
        String normalizedValue
    );

    Optional<PixKey> findKeyByIdAndAccountId(
        UUID keyId,
        UUID accountId
    );

    List<PixKey> findActiveKeysByAccountId(
        UUID accountId
    );

    long countActiveKeysByAccountId(
        UUID accountId
    );

    PixKey saveKey(
        PixKey key
    );

    Optional<PixPayment> findPaymentById(
        UUID paymentId
    );

    Optional<PixPayment> findPaymentForUpdate(
        UUID paymentId
    );

    Optional<PixPayment> findPaymentBySourceAndIdempotencyKey(
        UUID sourceAccountId,
        UUID idempotencyKey
    );

    List<PixPayment> findScheduledPaymentsBySource(
        UUID sourceAccountId,
        int limit
    );

    List<PixPayment> findScheduledPaymentsByRecurrence(
        UUID sourceAccountId,
        UUID recurrenceId
    );

    List<UUID> findDuePaymentIds(
        LocalDate today,
        int limit
    );

    PixPayment savePayment(
        PixPayment payment
    );

    List<PixPayment> savePayments(
        Collection<PixPayment> payments
    );

    List<PixFavorite> findFavoritesBySource(
        UUID sourceAccountId
    );

    Optional<PixFavorite> findFavoriteByIdAndSource(
        UUID favoriteId,
        UUID sourceAccountId
    );

    Optional<PixFavorite> findFavoriteBySourceAndKey(
        UUID sourceAccountId,
        PixKeyType keyType,
        String normalizedKey
    );

    Optional<PixFavorite> findUsableFavorite(
        UUID sourceAccountId,
        PixKeyType keyType,
        String normalizedKey
    );

    PixFavorite saveFavorite(
        PixFavorite favorite
    );

    void deleteFavorite(
        UUID favoriteId,
        UUID sourceAccountId
    );

    void deleteFavoritesByPixKeyId(
        UUID pixKeyId
    );
}