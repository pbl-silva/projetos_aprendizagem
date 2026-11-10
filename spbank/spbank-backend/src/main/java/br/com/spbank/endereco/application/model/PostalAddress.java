package br.com.spbank.endereco.application.model;

import java.util.Objects;

public record PostalAddress(
        String postalCode,
        String street,
        String complement,
        String district,
        String city,
        String state
) {
    public PostalAddress {
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(complement, "complement");
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(state, "state");
        if (!postalCode.matches("\\d{8}") || city.isBlank() || !state.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Invalid postal address");
        }
    }
}
