package br.com.spbank.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class CpfValidator
        implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context
    ) {
        if (value == null) {
            return true;
        }

        String cpf =
                value.replaceAll(
                        "\\D",
                        ""
                );

        if (cpf.length() != 11
                || cpf.chars()
                        .distinct()
                        .count() == 1) {

            return false;
        }

        return digit(
                cpf,
                9,
                10
        ) == cpf.charAt(9) - '0'
                && digit(
                        cpf,
                        10,
                        11
                ) == cpf.charAt(10) - '0';
    }

    private static int digit(
            String cpf,
            int length,
            int weight
    ) {
        int sum = 0;

        for (
                int i = 0;
                i < length;
                i++
        ) {
            sum +=
                    (cpf.charAt(i) - '0')
                            * (weight - i);
        }

        int remainder =
                11 - sum % 11;

        return remainder > 9
                ? 0
                : remainder;
    }
}