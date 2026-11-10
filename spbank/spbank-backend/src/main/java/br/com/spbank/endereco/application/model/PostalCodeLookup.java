package br.com.spbank.endereco.application.model;

import java.time.Instant;
import java.util.Objects;

public record PostalCodeLookup(
        Status status,
        String postalCode,
        String street,
        String complement,
        String district,
        String city,
        String state,
        String source,
        Instant consultedAt,
        String notice
) {
    public PostalCodeLookup {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(complement, "complement");
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(consultedAt, "consultedAt");
        Objects.requireNonNull(notice, "notice");
    }

    public enum Status {
        AVAILABLE,
        NOT_FOUND,
        UNAVAILABLE
    }
}
