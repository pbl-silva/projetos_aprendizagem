package br.com.spbank.autenticacao.application.model;

public record CustomerAddress(
        String postalCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state
) {

    @Override
    public String toString() {
        return "CustomerAddress[postalCode=[REDACTED], street=[REDACTED], "
                + "number=[REDACTED], complement=[REDACTED], district=[REDACTED], "
                + "city=" + city
                + ", state=" + state + "]";
    }
}