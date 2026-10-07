package br.com.spbank.pix.application.service;

import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.shared.application.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class PixKeyNormalizer {

    private static final Pattern EMAIL_LOCAL =
        Pattern.compile(
            "^[A-Z0-9!#$%&'*+/=?^_`{|}~.-]+$",
            Pattern.CASE_INSENSITIVE
        );

    private static final Pattern DOMAIN_LABEL =
        Pattern.compile(
            "^[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?$",
            Pattern.CASE_INSENSITIVE
        );

    private static final Pattern TOP_LEVEL_DOMAIN =
        Pattern.compile(
            "^[A-Z]{2,63}$",
            Pattern.CASE_INSENSITIVE
        );

    private static final Set<String> BRAZILIAN_AREA_CODES =
        Set.of(
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "21", "22", "24", "27", "28",
            "31", "32", "33", "34", "35", "37", "38",
            "41", "42", "43", "44", "45", "46", "47", "48", "49",
            "51", "53", "54", "55",
            "61", "62", "63", "64", "65", "66", "67", "68", "69",
            "71", "73", "74", "75", "77", "79",
            "81", "82", "83", "84", "85", "86", "87", "88", "89",
            "91", "92", "93", "94", "95", "96", "97", "98", "99"
        );

    private PixKeyNormalizer() {
    }

    public static String normalize(
        PixKeyType type,
        String raw
    ) {
        if (type == null) {
            throw invalid();
        }

        String value =
            Objects.requireNonNullElse(
                raw,
                ""
            ).trim();

        return switch (type) {
            case CPF_CNPJ -> document(value);
            case EMAIL -> email(value);
            case PHONE -> phone(value);
            case RANDOM -> random(value);
        };
    }

    public static String mask(
        PixKeyType type,
        String normalized
    ) {
        return switch (type) {
            case CPF_CNPJ ->
                "***" + tail(normalized, 4);

            case EMAIL -> {
                int at =
                    normalized.indexOf('@');

                yield normalized.substring(0, 1)
                    + "***"
                    + normalized.substring(at);
            }

            case PHONE ->
                "+***" + tail(normalized, 4);

            case RANDOM ->
                normalized.substring(0, 8)
                    + "-****-****-****-"
                    + tail(normalized, 4);
        };
    }

    public static String maskDocument(
        String document
    ) {
        return "***"
            + tail(document, 4);
    }

    public static String sha256(
        String value
    ) {
        try {
            byte[] hash =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                        value.getBytes(
                            StandardCharsets.UTF_8
                        )
                    );

            return HexFormat
                .of()
                .formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String document(
        String value
    ) {
        String digits =
            value.replaceAll(
                "\\D",
                ""
            );

        boolean valid =
            digits.length() == 11
                ? validCpf(digits)
                : digits.length() == 14
                    && validCnpj(digits);

        if (!valid) {
            throw invalid();
        }

        return digits;
    }

    private static String email(
        String value
    ) {
        String normalized =
            value.toLowerCase(
                Locale.ROOT
            );

        int at =
            normalized.indexOf('@');

        if (normalized.length() > 77
            || at <= 0
            || at != normalized.lastIndexOf('@')) {

            throw invalid();
        }

        String local =
            normalized.substring(
                0,
                at
            );

        String domain =
            normalized.substring(
                at + 1
            );

        if (local.length() > 64
            || local.startsWith(".")
            || local.endsWith(".")
            || local.contains("..")
            || !EMAIL_LOCAL
                .matcher(local)
                .matches()) {

            throw invalid();
        }

        String[] labels =
            domain.split(
                "\\.",
                -1
            );

        if (labels.length < 2
            || Arrays.stream(labels)
                .anyMatch(
                    label ->
                        !DOMAIN_LABEL
                            .matcher(label)
                            .matches()
                )
            || !TOP_LEVEL_DOMAIN
                .matcher(
                    labels[labels.length - 1]
                )
                .matches()) {

            throw invalid();
        }

        return normalized;
    }

    private static String phone(
        String value
    ) {
        String digits =
            value.replaceAll(
                "\\D",
                ""
            );

        if (digits.length() == 11) {
            digits =
                "55" + digits;
        }

        if (digits.length() != 13
            || !digits.startsWith("55")) {

            throw invalid();
        }

        String national =
            digits.substring(2);

        if (!BRAZILIAN_AREA_CODES.contains(
                national.substring(0, 2)
            )
            || national.charAt(2) != '9') {

            throw invalid();
        }

        return "+"
            + digits;
    }

    private static String random(
        String value
    ) {
        try {
            return UUID
                .fromString(value)
                .toString();

        } catch (RuntimeException ex) {
            throw invalid();
        }
    }

    private static String tail(
        String value,
        int size
    ) {
        return value.substring(
            Math.max(
                0,
                value.length() - size
            )
        );
    }

    private static boolean validCpf(
        String digits
    ) {
        if (allDigitsEqual(digits)) {
            return false;
        }

        int first =
            cpfDigit(
                digits,
                9
            );

        int second =
            cpfDigit(
                digits,
                10
            );

        return first
                == Character.digit(
                    digits.charAt(9),
                    10
                )
            && second
                == Character.digit(
                    digits.charAt(10),
                    10
                );
    }

    private static int cpfDigit(
        String digits,
        int length
    ) {
        int sum = 0;

        for (
            int index = 0;
            index < length;
            index++
        ) {
            sum +=
                Character.digit(
                    digits.charAt(index),
                    10
                )
                * (length + 1 - index);
        }

        int result =
            11 - sum % 11;

        return result >= 10
            ? 0
            : result;
    }

    private static boolean validCnpj(
        String digits
    ) {
        if (allDigitsEqual(digits)) {
            return false;
        }

        int first =
            cnpjDigit(
                digits,
                12
            );

        int second =
            cnpjDigit(
                digits,
                13
            );

        return first
                == Character.digit(
                    digits.charAt(12),
                    10
                )
            && second
                == Character.digit(
                    digits.charAt(13),
                    10
                );
    }

    private static int cnpjDigit(
        String digits,
        int length
    ) {
        int sum = 0;
        int weight =
            length - 7;

        for (
            int index = 0;
            index < length;
            index++
        ) {
            sum +=
                Character.digit(
                    digits.charAt(index),
                    10
                )
                * weight--;

            if (weight == 1) {
                weight = 9;
            }
        }

        int remainder =
            sum % 11;

        return remainder < 2
            ? 0
            : 11 - remainder;
    }

    private static boolean allDigitsEqual(
        String digits
    ) {
        return digits
            .chars()
            .allMatch(
                value ->
                    value
                        == digits.charAt(0)
            );
    }

    private static BusinessException invalid() {
        return new BusinessException(
            "INVALID_PIX_KEY",
            "pix.key.invalid"
        );
    }
}