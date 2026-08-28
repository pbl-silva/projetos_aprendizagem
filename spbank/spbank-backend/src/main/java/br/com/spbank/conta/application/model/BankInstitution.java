package br.com.spbank.conta.application.model;

public record BankInstitution(
        String studyCode,
        String name,
        boolean internal,
        boolean active
) {
}