package br.com.spbank.cartao.application.port.in;

import br.com.spbank.cartao.application.model.*;
import java.util.List;
import java.util.UUID;

public interface CardInstrumentUseCase {
    List<CardSummary> cards(UUID customerId);
    CardSummary issue(UUID customerId, CardBrand brand);
    CardSummary activate(UUID customerId, UUID cardId, String password, String pin, String confirmation);
    CardSummary changePin(UUID customerId, UUID cardId, String currentPin, String newPin, String confirmation);
    CardSummary recoverPin(UUID customerId, UUID cardId, String password, String newPin, String confirmation);
    CardDetails details(UUID customerId, UUID cardId, String pin);
    CardSummary block(UUID customerId, UUID cardId, String reason);
    CardSummary cancel(UUID customerId, UUID cardId);
    CardSummary replace(UUID customerId, UUID cardId);
    List<CardSummary> administrativeCards(UUID administratorId, UUID customerId);
    CardSummary administrativeBlock(UUID administratorId, UUID customerId, UUID cardId,
            String cause, boolean blocked, String reason);
}
