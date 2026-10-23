package br.com.spbank.cartao.application.model;

import java.time.LocalDate;

public record CardDetails(String holderName, String pan, String cvv, LocalDate expiresOn) {
    @Override public String toString() { return "CardDetails[REDACTED]"; }
}
