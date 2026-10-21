
package br.com.spbank.cartao.application.service;

import br.com.spbank.shared.application.model.CustomerPlan;

import java.math.BigDecimal;
import java.util.Objects;

public final class CardCreditPolicy {

    private static final BigDecimal STANDARD_CEILING =
            new BigDecimal("5000.00");

    private static final BigDecimal PLUS_CEILING =
            new BigDecimal("20000.00");

    private CardCreditPolicy() {
    }

    public static BigDecimal ceiling(
            CustomerPlan plan
    ) {

        return switch (
                Objects.requireNonNull(
                        plan,
                        "plan"
                )
        ) {

            case STANDARD -> STANDARD_CEILING;

            case PLUS -> PLUS_CEILING;
        };
    }
}