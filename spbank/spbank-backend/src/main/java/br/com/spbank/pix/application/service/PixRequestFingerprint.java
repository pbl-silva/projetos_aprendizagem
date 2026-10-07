package br.com.spbank.pix.application.service;

import br.com.spbank.pix.application.port.in.PixCreationCommand;

import java.util.Locale;

public final class PixRequestFingerprint {

    private PixRequestFingerprint() {
    }

    public static String calculate(
        PixCreationCommand command,
        String normalizedKey
    ) {
        String canonical = String.join(
            "|",
            command.sourceAccountId().toString(),
            command.keyType().name(),
            normalizedKey,
            command.amount()
                .stripTrailingZeros()
                .toPlainString(),
            command.scheduledFor() == null
                ? ""
                : command.scheduledFor().toString(),
            command.frequency() == null
                ? ""
                : command.frequency().name(),
            command.occurrences() == null
                ? ""
                : command.occurrences().toString(),
            Boolean.toString(
                command.saveFavorite()
            ).toLowerCase(Locale.ROOT)
        );

        return PixKeyNormalizer.sha256(
            canonical
        );
    }
}