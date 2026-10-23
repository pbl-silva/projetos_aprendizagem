package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CardInstrumentDtos.AdministrativeBlock;
import br.com.spbank.cartao.application.model.CardSummary;
import br.com.spbank.cartao.application.port.in.CardInstrumentUseCase;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/cards/customers/{customerId}")
public final class AdministrativeCardInstrumentController {
    private final CardInstrumentUseCase cards;
    public AdministrativeCardInstrumentController(CardInstrumentUseCase cards) { this.cards = cards; }

    @GetMapping public List<CardSummary> list(@RequestAttribute(ADMINISTRATOR_ID) UUID administratorId,
            @PathVariable UUID customerId) { return cards.administrativeCards(administratorId, customerId); }
    @PutMapping("/{cardId}") public CardSummary block(@RequestAttribute(ADMINISTRATOR_ID) UUID administratorId,
            @PathVariable UUID customerId, @PathVariable UUID cardId, @Valid @RequestBody AdministrativeBlock body) {
        return cards.administrativeBlock(administratorId, customerId, cardId, body.cause(), body.blocked(), body.reason());
    }
}
