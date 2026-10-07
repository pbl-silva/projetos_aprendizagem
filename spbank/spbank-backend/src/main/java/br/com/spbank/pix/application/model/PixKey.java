package br.com.spbank.pix.application.model;

import java.time.Instant;
import java.util.UUID;

public final class PixKey {

    private final UUID id;
    private final UUID accountId;
    private final PixKeyType type;
    private final String normalizedValue;
    private boolean active;
    private final Instant createdAt;

    public PixKey(
        UUID id,
        UUID accountId,
        PixKeyType type,
        String normalizedValue,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.normalizedValue = normalizedValue;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static PixKey create(
        UUID accountId,
        PixKeyType type,
        String normalizedValue,
        Instant now
    ) {
        return new PixKey(
            UUID.randomUUID(),
            accountId,
            type,
            normalizedValue,
            true,
            now
        );
    }

    public void deactivate() {
        active = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public PixKeyType getType() {
        return type;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}