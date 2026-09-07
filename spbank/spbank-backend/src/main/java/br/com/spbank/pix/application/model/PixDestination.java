package br.com.spbank.pix.application.model;

import java.util.Objects;
import java.util.UUID;

public record PixDestination(
    PixDestinationScope scope,
    UUID accountId,
    String holderName,
    String maskedDocument,
    String maskedKey,
    String bankCode,
    String bankName
) {

    public PixDestination {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(holderName);
        Objects.requireNonNull(maskedKey);

        if (scope == PixDestinationScope.INTERNAL
            && accountId == null) {
            throw new IllegalArgumentException(
                "Conta de destino é obrigatória para PIX interno"
            );
        }

        if (scope == PixDestinationScope.EXTERNAL
            && accountId != null) {
            throw new IllegalArgumentException(
                "Conta de destino não deve ser informada para PIX externo"
            );
        }
    }

    public boolean isInternal() {
        return scope == PixDestinationScope.INTERNAL;
    }

    public boolean isExternal() {
        return scope == PixDestinationScope.EXTERNAL;
    }
}