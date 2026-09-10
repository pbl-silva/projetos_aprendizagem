package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixKeyType;

import java.util.Objects;
import java.util.UUID;

public record PixKeyRegistrationCommand(
    UUID accountId,
    PixKeyType type,
    String value
) {

    public PixKeyRegistrationCommand {
        Objects.requireNonNull(
            accountId,
            "accountId"
        );

        Objects.requireNonNull(
            type,
            "type"
        );
    }

    @Override
    public String toString() {
        return "PixKeyRegistrationCommand[accountId="
            + accountId
            + ", type="
            + type
            + ", value=[REDACTED]]";
    }
}