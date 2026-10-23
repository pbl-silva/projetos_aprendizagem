package br.com.spbank.cartao.application.model;

import java.time.Instant;
import java.util.UUID;

public record CardBlockAudit(UUID id, UUID cardId, UUID actorId, String action, String reason, Instant occurredAt) {
}
