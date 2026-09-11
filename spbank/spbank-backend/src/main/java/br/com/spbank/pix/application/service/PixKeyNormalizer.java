package br.com.spbank.pix.application.service;

import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.shared.application.exception.BusinessException;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class PixKeyNormalizer {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        );

    private static final Pattern PHONE_PATTERN =
        Pattern.compile(
            "^[1-9][0-9]9[0-9]{8}$"
        );

    public String normalize(
        PixKeyType type,
        String value
    ) {
        Objects.requireNonNull(type, "type");

        if (value == null || value.isBlank()) {
            throw invalidKey();
        }

        return switch (type) {
            case CPF_CNPJ ->
                normalizeDocument(value);

            case EMAIL ->
                normalizeEmail(value);

            case PHONE ->
                normalizePhone(value);

            case RANDOM ->
                normalizeRandom(value);
        };
    }

    private String normalizeDocument(
        String value
    ) {
        String digits =
            value.replaceAll("\\D", "");

        if (!isValidDocument(digits)) {
            throw invalidKey();
        }

        return digits;
    }

    private String normalizeEmail(
        String value
    ) {
        String normalized =
            value.trim()
                .toLowerCase(Locale.ROOT);

        if (normalized.length() > 254
            || !EMAIL_PATTERN
                .matcher(normalized)
                .matches()) {

            throw invalidKey();
        }

        return normalized;
    }

    private String normalizePhone(
        String value
    ) {
        String digits =
            value.replaceAll("\\D", "");

        if (digits.length() == 13
            && digits.startsWith("55")) {

            digits = digits.substring(2);
        }

        if (!PHONE_PATTERN
            .matcher(digits)
            .matches()) {

            throw invalidKey();
        }

        return digits;
    }

    private String normalizeRandom(
        String value
    ) {
        try {
            return UUID.fromString(
                value.trim()
            ).toString();

        } catch (IllegalArgumentException ex) {
            throw invalidKey();
        }
    }

    private boolean isValidDocument(
        String number
    ) {
        if (number.length() != 11
            && number.length() != 14) {

            return false;
        }

        if (number.chars()
            .distinct()
            .count() == 1) {

            return false;
        }

        return number.length() == 11
            ? isValidCpf(number)
            : isValidCnpj(number);
    }

    private boolean isValidCpf(
        String number
    ) {
        return cpfDigit(
            number,
            9,
            10
        ) == number.charAt(9) - '0'

            && cpfDigit(
                number,
                10,
                11
            ) == number.charAt(10) - '0';
    }

    private int cpfDigit(
        String number,
        int length,
        int weight
    ) {
        int sum = 0;

        for (int i = 0; i < length; i++) {
            sum +=
                (number.charAt(i) - '0')
                    * (weight - i);
        }

        int remainder =
            11 - sum % 11;

        return remainder > 9
            ? 0
            : remainder;
    }

    private boolean isValidCnpj(
        String number
    ) {
        int[] firstWeights = {
            5, 4, 3, 2,
            9, 8, 7, 6,
            5, 4, 3, 2
        };

        int[] secondWeights = {
            6, 5, 4, 3, 2,
            9, 8, 7, 6,
            5, 4, 3, 2
        };

        return cnpjDigit(
            number,
            12,
            firstWeights
        ) == number.charAt(12) - '0'

            && cnpjDigit(
                number,
                13,
                secondWeights
            ) == number.charAt(13) - '0';
    }

    private int cnpjDigit(
        String number,
        int length,
        int[] weights
    ) {
        int sum = 0;

        for (int i = 0; i < length; i++) {
            sum +=
                (number.charAt(i) - '0')
                    * weights[i];
        }

        int remainder =
            sum % 11;

        return remainder < 2
            ? 0
            : 11 - remainder;
    }

    private BusinessException invalidKey() {
        return new BusinessException(
            "INVALID_PIX_KEY",
            "pix.key.invalid"
        );
    }
}