
package br.com.spbank.cartao.application.model;

import java.math.BigDecimal;
import java.util.Objects;

public record CardCreditLine(
        CardBrand brand,
        BigDecimal allocated,
        BigDecimal committed
) {

    public CardCreditLine {

        Objects.requireNonNull(
                brand,
                "brand"
        );

        allocated = CardMoney.nonNegative(
                allocated
        );

        committed = CardMoney.nonNegative(
                committed
        );
    }

    public BigDecimal available() {

        return allocated
                .subtract(committed)
                .max(BigDecimal.ZERO)
                .setScale(2);
    }

    public BigDecimal excess() {

        return committed
                .subtract(allocated)
                .max(BigDecimal.ZERO)
                .setScale(2);
    }
}
