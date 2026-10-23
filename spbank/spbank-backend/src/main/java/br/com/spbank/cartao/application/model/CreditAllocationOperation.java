package br.com.spbank.cartao.application.model;

import java.util.UUID;

public record CreditAllocationOperation(
        UUID customerId, UUID key, String fingerprint, CreditAllocationResult result) {
}
