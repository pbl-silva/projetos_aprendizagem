package br.com.spbank.pix.application.model;

import java.util.UUID;

public record PixDestination(
    PixRouting routing,
    UUID accountId,
    String recipientName,
    String maskedRecipientDocument,
    String bankCode,
    String bankName
) {

    public static PixDestination internal(
        UUID accountId,
        String recipientName,
        String maskedDocument,
        String bankCode
    ) {
        return new PixDestination(
            PixRouting.INTERNAL,
            accountId,
            recipientName,
            maskedDocument,
            bankCode,
            "SPBank"
        );
    }

    public static PixDestination external(
        String recipientName,
        String maskedDocument,
        String bankCode,
        String bankName
    ) {
        return new PixDestination(
            PixRouting.EXTERNAL,
            null,
            recipientName,
            maskedDocument,
            bankCode,
            bankName
        );
    }
}