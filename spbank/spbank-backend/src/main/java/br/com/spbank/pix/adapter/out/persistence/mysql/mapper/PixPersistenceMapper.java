package br.com.spbank.pix.adapter.out.persistence.mysql.mapper;

import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixFavoriteData;
import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixKeyData;
import br.com.spbank.pix.adapter.out.persistence.mysql.data.PixPaymentData;
import br.com.spbank.pix.application.model.PixFavorite;
import br.com.spbank.pix.application.model.PixKey;
import br.com.spbank.pix.application.model.PixPayment;

public final class PixPersistenceMapper {

    private PixPersistenceMapper() {
    }

    public static PixKey toDomain(
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

    public static PixKeyData toData(
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

    public static PixPayment toDomain(
        PixPaymentData data
    ) {
        return new PixPayment(
            data.getId(),
            data.getOriginAccountId(),
            data.getDestinationAccountId(),
            data.getDestinationScope(),
            data.getKeyType(),
            data.getMaskedKey(),
            data.getKeyHash(),
            data.getHolderName(),
            data.getMaskedDocument(),
            data.getBankCode(),
            data.getBankName(),
            data.getAmount(),
            data.getStatus(),
            data.getIdempotencyKey(),
            data.getRequestHash(),
            data.getRequestedAt(),
            data.getScheduledFor(),
            data.getProcessedAt(),
            data.getFailureCode(),
            data.getFailureMessage(),
            data.getRecurrenceId(),
            data.getRecurrenceFrequency(),
            data.getOccurrenceNumber(),
            data.getTotalOccurrences(),
            data.getSettlementReference()
        );
    }

    public static PixPaymentData toData(
        PixPayment payment
    ) {
        return new PixPaymentData(
            payment.getId(),
            payment.getOriginAccountId(),
            payment.getDestinationAccountId(),
            payment.getDestinationScope(),
            payment.getKeyType(),
            payment.getMaskedKey(),
            payment.getKeyHash(),
            payment.getHolderName(),
            payment.getMaskedDocument(),
            payment.getBankCode(),
            payment.getBankName(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getIdempotencyKey(),
            payment.getRequestHash(),
            payment.getRequestedAt(),
            payment.getScheduledFor(),
            payment.getProcessedAt(),
            payment.getSettlementReference(),
            payment.getFailureCode(),
            payment.getFailureMessage(),
            payment.getRecurrenceId(),
            payment.getRecurrenceFrequency(),
            payment.getOccurrenceNumber(),
            payment.getTotalOccurrences()
        );
    }

    public static PixFavorite toDomain(
        PixFavoriteData data
    ) {
        return new PixFavorite(
            data.getId(),
            data.getOriginAccountId(),
            data.getPixKeyId(),
            data.getNormalizedKeyValue(),
            data.getDestinationScope(),
            data.getHolderName(),
            data.getMaskedKey(),
            data.getKeyType(),
            data.getBankCode(),
            data.getBankName(),
            data.getMaskedDocument(),
            data.getCreatedAt()
        );
    }

    public static PixFavoriteData toData(
        PixFavorite favorite
    ) {
        return new PixFavoriteData(
            favorite.getId(),
            favorite.getOriginAccountId(),
            favorite.getPixKeyId(),
            favorite.getNormalizedKeyValue(),
            favorite.getDestinationScope(),
            favorite.getHolderName(),
            favorite.getMaskedKey(),
            favorite.getKeyType(),
            favorite.getBankCode(),
            favorite.getBankName(),
            favorite.getMaskedDocument(),
            favorite.getCreatedAt()
        );
    }
}