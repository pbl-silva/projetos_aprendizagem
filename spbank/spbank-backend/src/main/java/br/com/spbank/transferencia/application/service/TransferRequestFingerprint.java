package br.com.spbank.transferencia.application.service;

import br.com.spbank.transferencia.application.port.in.CreateTransferCommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

public final class TransferRequestFingerprint {

    private static final String SEPARATOR = "\u001F";

    private TransferRequestFingerprint() {
    }

    public static String calculate(
            CreateTransferCommand command
    ) {

        String canonical = String.join(
                SEPARATOR,

                command.sourceAccountId().toString(),

                text(
                        command.recipientName()
                ),

                digits(
                        command.recipientDocument()
                ),

                alphanumeric(
                        command.target().bankCode()
                ),

                alphanumeric(
                        command.target().branch()
                ),

                alphanumeric(
                        command.target().accountNumber()
                ),

                command.target()
                        .accountType()
                        .name(),

                command.amount()
                        .stripTrailingZeros()
                        .toPlainString(),

                command.scheduledFor() == null
                        ? ""
                        : command.scheduledFor().toString()
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

    private static String digits(String value) {

        return value.replaceAll("\\D", "");
    }

    private static String alphanumeric(String value) {

        return value
                .replaceAll(
                        "[^0-9A-Za-z]",
                        ""
                )
                .toUpperCase(Locale.ROOT);
    }

    private static String text(String value) {

        String ascii = Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "");

        return ascii
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
}