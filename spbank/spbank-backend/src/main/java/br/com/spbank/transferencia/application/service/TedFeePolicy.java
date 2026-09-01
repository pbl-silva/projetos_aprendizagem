package br.com.spbank.transferencia.application.service;

import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.AccountPlan;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public final class TedFeePolicy {

    private final TransferProperties properties;

    public TedFeePolicy(TransferProperties properties) {
        this.properties = properties;
    }

    public BigDecimal feeFor(
            Account source,
            long completedTedsInMonth
    ) {

        if (source.getAccountPlan() == AccountPlan.PLUS
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
            Account source,
            long completedTedsInMonth
    ) {

        if (source.getAccountPlan() != AccountPlan.PLUS) {
            return 0;
        }

        return Math.max(
                0,
                properties.ted().plusFreePerMonth()
                        - Math.toIntExact(completedTedsInMonth)
        );
    }
}