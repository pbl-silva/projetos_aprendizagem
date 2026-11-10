package br.com.spbank.mercado.application.model;

import java.math.BigDecimal;
import java.util.Objects;

public record MarketRate(String name, BigDecimal annualPercent) {
    public MarketRate {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(annualPercent, "annualPercent");
        if (name.isBlank() || annualPercent.signum() < 0) {
            throw new IllegalArgumentException("Invalid market rate");
        }
    }
}
