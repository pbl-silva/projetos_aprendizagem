package br.com.spbank.pix.application.service;

import br.com.spbank.pix.application.port.in.PixCreationCommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public final class PixRequestFingerprint {

    private static final String SEPARATOR = "\u001F";

    private PixRequestFingerprint() {
    }

    public static String calculate(
        PixCreationCommand command,
        String normalizedKey
    ) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(
            normalizedKey,
            "normalizedKey"
        );

        String canonical = String.join(
            SEPARATOR,

            command.sourceAccountId()
                .toString(),

            command.keyType()
                .name(),

            normalizedKey,

            command.amount()
                .stripTrailingZeros()
                .toPlainString(),

            command.scheduledFor() == null
                ? ""
                : command.scheduledFor()
                    .toString(),

            normalizeRecurrence(
                command.recurrenceFrequency()
            ),

            command.totalOccurrences() == null
                ? ""
                : command.totalOccurrences()
                    .toString(),

            Boolean.toString(
                command.saveFavorite()
            )
        );

        try {
            byte[] digest = MessageDigest
                .getInstance("SHA-256")
                .digest(
                    canonical.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return HexFormat
                .of()
                .formatHex(digest);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                "SHA-256 indisponível",
                ex
            );
        }
    }

    private static String normalizeRecurrence(
        String recurrenceFrequency
    ) {
        if (recurrenceFrequency == null) {
            return "";
        }

        return recurrenceFrequency
            .trim()
            .toUpperCase(Locale.ROOT);
    }
}