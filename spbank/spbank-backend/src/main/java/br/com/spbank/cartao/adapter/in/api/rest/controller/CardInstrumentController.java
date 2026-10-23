package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CardInstrumentDtos.*;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.port.in.CardInstrumentUseCase;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cards")
public final class CardInstrumentController {
    private final CardInstrumentUseCase cards;
    private final br.com.spbank.cartao.application.usecase.CardFinanceService finance;
    public CardInstrumentController(CardInstrumentUseCase cards,br.com.spbank.cartao.application.usecase.CardFinanceService finance) { this.cards = cards; this.finance=finance; }

    @GetMapping public List<CardSummary> list(@RequestAttribute(CUSTOMER_ID) UUID customerId) {
        return cards.cards(customerId);
    }
    @PostMapping public CardSummary issue(@RequestAttribute(CUSTOMER_ID) UUID customerId, @Valid @RequestBody Issue body) {
        return finance.issue(customerId, body.brand(),body.dueDay());
    }
    @PostMapping("/{cardId}/activate") public CardSummary activate(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @PathVariable UUID cardId, @Valid @RequestBody Activate body) {
        return cards.activate(customerId, cardId, body.confirmationPassword(), body.pin(), body.pinConfirmation());
    }
    @PutMapping("/{cardId}/pin") public CardSummary changePin(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @PathVariable UUID cardId, @Valid @RequestBody ChangePin body) {
        return cards.changePin(customerId, cardId, body.currentPin(), body.newPin(), body.pinConfirmation());
    }
    @PutMapping("/{cardId}/pin/recovery") public CardSummary recover(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @PathVariable UUID cardId, @Valid @RequestBody RecoverPin body) {
        return cards.recoverPin(customerId, cardId, body.confirmationPassword(), body.newPin(), body.pinConfirmation());
    }
    @PostMapping("/{cardId}/details") public ResponseEntity<CardDetails> reveal(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @PathVariable UUID cardId, @Valid @RequestBody Reveal body) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("Pragma", "no-cache")
            .body(cards.details(customerId, cardId, body.pin()));
    }
    @PostMapping("/{cardId}/block") public CardSummary block(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @PathVariable UUID cardId, @Valid @RequestBody Block body) {
        return cards.block(customerId, cardId, body.reason());
    }
    @PostMapping("/{cardId}/cancel") public CardSummary cancel(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @PathVariable UUID cardId) { return cards.cancel(customerId, cardId); }
    @PostMapping("/{cardId}/replacement") public CardSummary replace(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @PathVariable UUID cardId) { return cards.replace(customerId, cardId); }
}
