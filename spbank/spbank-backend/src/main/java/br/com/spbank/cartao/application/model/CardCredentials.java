package br.com.spbank.cartao.application.model;

public record CardCredentials(String encryptedPan, String encryptedCvv,
        String panFingerprint, String lastFour) {
    @Override public String toString() { return "CardCredentials[REDACTED]"; }
}
