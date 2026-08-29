package br.com.spbank.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class CpfOrCnpjValidator
        implements ConstraintValidator<CpfOrCnpj, String> {

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context
    ) {

        if (value == null) {
            return true;
        }

        String number =
                value.replaceAll("\\D", "");

        if (number.chars()
                .distinct()
                .count() == 1) {

            return false;
        }

        return number.length() == 11
                ? cpf(number)
                : number.length() == 14
                    && cnpj(number);
    }

    private boolean cpf(String number) {

        return digit(number, 9, 10)
                == number.charAt(9) - '0'
                && digit(number, 10, 11)
                == number.charAt(10) - '0';
    }

    private int digit(
            String number,
            int length,
            int weight
    ) {

        int sum = 0;

        for (int i = 0; i < length; i++) {

            sum += (number.charAt(i) - '0')
                    * (weight - i);
        }

        int remainder =
                11 - sum % 11;

        return remainder > 9
                ? 0
                : remainder;
    }

    private boolean cnpj(String number) {

        int[] weights1 = {
                5, 4, 3, 2,
                9, 8, 7, 6,
                5, 4, 3, 2
        };

        int[] weights2 = {
                6, 5, 4, 3, 2,
                9, 8, 7, 6,
                5, 4, 3, 2
        };

        return cnpjDigit(
                number,
                12,
                weights1
        ) == number.charAt(12) - '0'

                && cnpjDigit(
                        number,
                        13,
                        weights2
                ) == number.charAt(13) - '0';
    }

    private int cnpjDigit(
            String number,
            int length,
            int[] weights
    ) {

        int sum = 0;

        for (int i = 0; i < length; i++) {

            sum += (number.charAt(i) - '0')
                    * weights[i];
        }

        int remainder =
                sum % 11;

        return remainder < 2
                ? 0
                : 11 - remainder;
    }
}