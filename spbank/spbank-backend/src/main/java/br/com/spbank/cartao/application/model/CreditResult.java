package br.com.spbank.cartao.application.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
/** Retrato imutável da operação: também é a resposta de uma repetição idempotente. */
public record CreditResult(UUID operationId, UUID actorId, String action,
        CreditRequest request, BigDecimal approvedTotal, Instant occurredAt) { }
