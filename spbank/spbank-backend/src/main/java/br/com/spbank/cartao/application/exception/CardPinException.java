package br.com.spbank.cartao.application.exception;

import br.com.spbank.shared.application.exception.BusinessException;

/** Only this refusal commits the failed PIN counter; other failures roll back. */
public final class CardPinException extends BusinessException {
    public CardPinException(boolean blocked) {
        super(blocked ? "CARD_PIN_BLOCKED" : "CARD_PIN_INVALID",
            blocked ? "card.instrument.pin-blocked" : "card.instrument.pin-invalid");
    }
}
