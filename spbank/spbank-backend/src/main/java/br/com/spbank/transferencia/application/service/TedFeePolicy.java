package br.com.spbank.transferencia.application.service;

import br.com.spbank.shared.application.model.CustomerPlan;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public final class TedFeePolicy {

    private final TransferProperties properties;

    public TedFeePolicy(TransferProperties properties) {
        this.properties = properties;
    }

    public BigDecimal feeFor(
            CustomerPlan plan,
            long completedTedsInMonth
    ) {
        Objects.requireNonNull(
                plan,
                "plan"
        );

        if (plan == CustomerPlan.PLUS
                && completedTedsInMonth
                < properties.ted().plusFreePerMonth()) {

            return BigDecimal.ZERO.setScale(2);
        }

        return properties
                .ted()
                .fee()
                .setScale(2);
    }

    public int remainingFreeTeds(
            CustomerPlan plan,
            long completedTedsInMonth
    ) {
        Objects.requireNonNull(
                plan,
                "plan"
        );

        if (plan != CustomerPlan.PLUS) {
            return 0;
        }

        return Math.max(
                0,
                properties.ted().plusFreePerMonth()
                        - Math.toIntExact(completedTedsInMonth)
        );
    }
}