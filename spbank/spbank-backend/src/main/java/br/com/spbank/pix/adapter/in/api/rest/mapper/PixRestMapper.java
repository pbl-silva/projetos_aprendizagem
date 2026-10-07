package br.com.spbank.pix.adapter.in.api.rest.mapper;

import br.com.spbank.pix.adapter.in.api.rest.dto.PixFavoriteDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixKeyDto;
import br.com.spbank.pix.adapter.in.api.rest.dto.PixReceiptDto;
import br.com.spbank.pix.application.model.PixFavorite;
import br.com.spbank.pix.application.model.PixKey;
import br.com.spbank.pix.application.model.PixPayment;

public final class PixRestMapper {

    private PixRestMapper() {
    }

    public static PixKeyDto toDto(
        PixKey value
    ) {
        return new PixKeyDto(
            value.getId(),
            value.getType(),
            value.getNormalizedValue(),
            value.getCreatedAt()
        );
    }

    public static PixFavoriteDto toDto(
        PixFavorite value
    ) {
        return new PixFavoriteDto(
            value.id(),
            value.pixKeyId(),
            value.recipientName(),
            value.keyValue(),
            value.maskedKey(),
            value.keyType(),
            value.routing(),
            value.bankCode(),
            value.bankName(),
            value.maskedRecipientDocument(),
            value.createdAt()
        );
    }

    public static PixReceiptDto toDto(
        PixPayment value
    ) {
        return new PixReceiptDto(
            value.getId(),
            value.getStatus(),
            value.getAmount(),
            value.getRecipientName(),
            value.getMaskedRecipientDocument(),
            value.getKeyType(),
            value.getMaskedKey(),
            value.getDestinationBankCode(),
            value.getDestinationBankName(),
            value.getSettlementReference(),
            value.getScheduledFor(),
            value.getRequestedAt(),
            value.getProcessedAt(),
            value.getFailureCode(),
            value.getRecurrenceId(),
            value.getFrequency(),
            value.getOccurrenceNumber(),
            value.getTotalOccurrences()
        );
    }
}