package br.com.spbank.cartao.application.service;

import br.com.spbank.cartao.application.model.*;
import java.time.*;
import java.util.UUID;

/** Shared construction for initial issue, cancellation replacement and automatic renewal. */
public final class CardInstrumentFactory {
    private CardInstrumentFactory() {}

    public static CardInstrument issue(UUID customerId, String holderName, CardBrand brand,
            CardInstrument previous, CardCredentials credentials, LocalDate today, Instant now) {
        return new CardInstrument(UUID.randomUUID(), customerId, brand,
            previous == null ? null : previous.id(), holderName, credentials.lastFour(),
            credentials.panFingerprint(), credentials.encryptedPan(), credentials.encryptedCvv(), null,
            CardState.PENDING_ACTIVATION, 0, false,
            previous != null && previous.voluntaryBlocked(), previous != null && previous.administrativeBlocked(),
            previous == null ? null : previous.voluntaryReason(),
            previous == null ? null : previous.administrativeReason(),
            YearMonth.from(today).plusYears(3).atEndOfMonth(), now);
    }
}
