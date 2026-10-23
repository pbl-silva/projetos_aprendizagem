package br.com.spbank.cartao.application.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardInstrument(UUID id, UUID customerId, CardBrand brand, UUID previousCardId,
        String holderName, String lastFour, String panFingerprint, String encryptedPan,
        String encryptedCvv, String pinHash, CardState state, int pinFailures,
        boolean pinBlocked, boolean voluntaryBlocked, boolean administrativeBlocked,
        String voluntaryReason, String administrativeReason, LocalDate expiresOn, Instant createdAt) {

    public CardState stateAt(LocalDate today) {
        return (state == CardState.ACTIVE || state == CardState.PENDING_ACTIVATION)
            && today.isAfter(expiresOn) ? CardState.EXPIRED : state;
    }

    public CardInstrument withState(CardState next) {
        return new CardInstrument(id, customerId, brand, previousCardId, holderName, lastFour,
            panFingerprint, encryptedPan, encryptedCvv, pinHash, next, pinFailures, pinBlocked,
            voluntaryBlocked, administrativeBlocked, voluntaryReason, administrativeReason, expiresOn, createdAt);
    }

    public CardInstrument withPin(String hash, int failures, boolean blocked) {
        return new CardInstrument(id, customerId, brand, previousCardId, holderName, lastFour,
            panFingerprint, encryptedPan, encryptedCvv, hash, state, failures, blocked,
            voluntaryBlocked, administrativeBlocked, voluntaryReason, administrativeReason, expiresOn, createdAt);
    }

    public CardInstrument withBlocks(boolean voluntary, boolean administrative,
            String voluntaryReason, String administrativeReason) {
        return new CardInstrument(id, customerId, brand, previousCardId, holderName, lastFour,
            panFingerprint, encryptedPan, encryptedCvv, pinHash, state, pinFailures, pinBlocked,
            voluntary, administrative, voluntaryReason, administrativeReason, expiresOn, createdAt);
    }

    @Override public String toString() {
        return "CardInstrument[id=" + id + ", brand=" + brand + ", lastFour=" + lastFour
            + ", state=" + state + ", credentials=[REDACTED]]";
    }
}
