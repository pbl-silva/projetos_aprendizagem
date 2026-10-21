
package br.com.spbank.cartao.application.model;

import br.com.spbank.cartao.application.service.CardCreditPolicy;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.model.CustomerPlan;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CustomerCardCredit(
        UUID customerId,
        BigDecimal approvedTotal,
        Map<CardBrand, CardCreditLine> lines
) {

    public CustomerCardCredit {

        Objects.requireNonNull(
                customerId,
                "customerId"
        );

        approvedTotal = CardMoney.nonNegative(
                approvedTotal
        );

        Objects.requireNonNull(
                lines,
                "lines"
        );

        if (lines.size() != CardBrand.values().length) {
            throw new IllegalArgumentException(
                    "É necessária uma linha para cada bandeira"
            );
        }

        var validated =
                new EnumMap<CardBrand, CardCreditLine>(
                        CardBrand.class
                );

        for (CardBrand brand : CardBrand.values()) {

            CardCreditLine line = Objects.requireNonNull(
                    lines.get(brand),
                    "Linha ausente: " + brand
            );

            if (line.brand() != brand) {
                throw new IllegalArgumentException(
                        "Bandeira incompatível: " + brand
                );
            }

            validated.put(
                    brand,
                    line
            );
        }

        lines = Map.copyOf(validated);
    }

    public static CustomerCardCredit initial(
            UUID customerId
    ) {

        var initialLines =
                new EnumMap<CardBrand, CardCreditLine>(
                        CardBrand.class
                );

        for (CardBrand brand : CardBrand.values()) {

            initialLines.put(
                    brand,
                    new CardCreditLine(
                            brand,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    )
            );
        }

        return new CustomerCardCredit(
                customerId,
                BigDecimal.ZERO,
                initialLines
        );
    }

    public BigDecimal allocatedTotal() {

        return lines.values()
                .stream()
                .map(CardCreditLine::allocated)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    public BigDecimal unallocatedTotal() {

        return approvedTotal.subtract(
                allocatedTotal()
        );
    }

    public boolean requiresAdjustment(
            CustomerPlan plan
    ) {

        return allocatedTotal().compareTo(
                CardCreditPolicy.ceiling(plan)
        ) > 0;
    }

    public CustomerCardCredit acceptApprovedTotal(
            BigDecimal newApprovedTotal,
            CustomerPlan plan
    ) {

        Objects.requireNonNull(
                plan,
                "plan"
        );

        BigDecimal total;

        try {
            total = CardMoney.positive(
                    newApprovedTotal
            );

        } catch (IllegalArgumentException ex) {
            throw error(
                    "INVALID_APPROVED_TOTAL",
                    "invalid-approved-total"
            );
        }

        if (total.compareTo(approvedTotal) <= 0
                || total.compareTo(
                        CardCreditPolicy.ceiling(plan)
                ) > 0) {

            throw error(
                    "INVALID_APPROVED_TOTAL",
                    "invalid-approved-total"
            );
        }

        return new CustomerCardCredit(
                customerId,
                total,
                lines
        );
    }

    public CustomerCardCredit redistribute(
            Map<CardBrand, BigDecimal> allocation,
            CustomerPlan plan
    ) {

        Objects.requireNonNull(
                plan,
                "plan"
        );

        if (allocation == null) {
            throw error(
                    "INVALID_AMOUNT",
                    "invalid-amount"
            );
        }

        var updatedLines =
                new EnumMap<CardBrand, CardCreditLine>(
                        CardBrand.class
                );

        for (CardBrand brand : CardBrand.values()) {

            BigDecimal amount;

            try {
                amount = CardMoney.nonNegative(
                        allocation.get(brand)
                );

            } catch (IllegalArgumentException ex) {
                throw error(
                        "INVALID_AMOUNT",
                        "invalid-amount"
                );
            }

            CardCreditLine currentLine =
                    lines.get(brand);

            if (amount.compareTo(
                    currentLine.committed()
            ) < 0) {

                throw error(
                        "BELOW_COMMITTED",
                        "below-committed"
                );
            }

            updatedLines.put(
                    brand,
                    new CardCreditLine(
                            brand,
                            amount,
                            currentLine.committed()
                    )
            );
        }

        CustomerCardCredit updated =
                new CustomerCardCredit(
                        customerId,
                        approvedTotal,
                        updatedLines
                );

        if (updated.allocatedTotal().compareTo(
                approvedTotal
        ) > 0) {

            throw error(
                    "EXCEEDS_APPROVED",
                    "exceeds-approved"
            );
        }

        if (updated.allocatedTotal().compareTo(
                CardCreditPolicy.ceiling(plan)
        ) > 0) {

            throw error(
                    "EXCEEDS_PLAN",
                    "exceeds-plan"
            );
        }

        return updated;
    }

    private static BusinessException error(
            String code,
            String key
    ) {

        return new BusinessException(
                "CARD_CREDIT_" + code,
                "card.credit." + key
        );
    }
}
