package br.com.spbank.cartao.application.model;
import java.util.UUID;
public record CreditOperation(UUID customerId, UUID key, String fingerprint, CreditResult result) { }
