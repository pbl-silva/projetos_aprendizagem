package br.com.spbank.mercado.application.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MarketRatesSnapshot(
        Status status,
        List<MarketRate> rates,
        String source,
        Instant consultedAt,
        String notice
) {
    public MarketRatesSnapshot {
        Objects.requireNonNull(status, "status");
        rates = List.copyOf(rates);
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(consultedAt, "consultedAt");
        Objects.requireNonNull(notice, "notice");
    }

    public enum Status {
        AVAILABLE,
        STALE,
        UNAVAILABLE
    }
}
