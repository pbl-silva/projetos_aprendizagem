
package br.com.spbank.cartao.application.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CardMoney {

    private CardMoney() {
    }

    public static BigDecimal nonNegative(
            BigDecimal value
    ) {
        BigDecimal money = exact(value);

        if (money.signum() < 0) {
            throw new IllegalArgumentException(
                    "O valor não pode ser negativo"
            );
        }

        return money;
    }

    public static BigDecimal positive(
            BigDecimal value
    ) {
        BigDecimal money = exact(value);

        if (money.signum() <= 0) {
            throw new IllegalArgumentException(
                    "O valor deve ser maior que zero"
            );
        }

        return money;
    }

    private static BigDecimal exact(
            BigDecimal value
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "O valor é obrigatório"
            );
        }

        if (value.scale() > 2) {
            throw new IllegalArgumentException(
                    "O valor deve ter no máximo duas casas decimais"
            );
        }

        return value.setScale(
                2,
                RoundingMode.UNNECESSARY
        );
    }
}
