package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;

import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditAllocationDto;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditAllocationSummaryDto;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditAllocationResultDto;
import br.com.spbank.cartao.application.port.in.CardCreditAllocationUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cards/credit/allocation")
public final class CardCreditAllocationController {
    private final CardCreditAllocationUseCase credit;

    public CardCreditAllocationController(CardCreditAllocationUseCase credit) {
        this.credit = credit;
    }

    @GetMapping
    public CreditAllocationSummaryDto allocation(@RequestAttribute(CUSTOMER_ID) UUID customerId) {
        return CreditAllocationSummaryDto.from(credit.allocation(customerId));
    }

    @PutMapping
    public CreditAllocationResultDto redistribute(@RequestAttribute(CUSTOMER_ID) UUID customerId,
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody CreditAllocationDto body) {
        return CreditAllocationResultDto.from(
            credit.redistribute(customerId, key, body.allocation(), body.confirmationPassword()));
    }
}
