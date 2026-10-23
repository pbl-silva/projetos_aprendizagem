package br.com.spbank.cartao.application.model;

import java.time.Instant;
import java.util.UUID;

public record CreditAllocationResult(
        UUID operationId, CustomerCardCredit credit, Instant occurredAt) {
}
