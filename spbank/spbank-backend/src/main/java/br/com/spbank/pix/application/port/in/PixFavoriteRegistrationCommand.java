package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixKeyType;

import java.util.UUID;

public record PixFavoriteRegistrationCommand(
    UUID sourceAccountId,
    PixKeyType keyType,
    String key,
    String recipientName,
    String bankCode,
    String recipientDocument
) {

    @Override
    public String toString() {
        return "PixFavoriteRegistrationCommand[sourceAccountId="
            + sourceAccountId
            + ", keyType=" + keyType
            + ", key=[REDACTED]"
            + ", recipientName=" + recipientName
            + ", bankCode=" + bankCode
            + ", recipientDocument=[REDACTED]]";
    }
}