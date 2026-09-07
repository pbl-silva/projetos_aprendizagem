package br.com.spbank.pix.application.model;

import java.time.Instant;
import java.util.Objects;
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
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.type = Objects.requireNonNull(type);
        this.normalizedValue = Objects.requireNonNull(normalizedValue);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
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

    public void deactivate() {
        this.active = false;
    }

    @Override
    public String toString() {
        return "PixKey[" +
            "id=" + id +
            ", accountId=" + accountId +
            ", type=" + type +
            ", normalizedValue=[REDACTED]" +
            ", active=" + active +
            ", createdAt=" + createdAt +
            ']';
    }
}