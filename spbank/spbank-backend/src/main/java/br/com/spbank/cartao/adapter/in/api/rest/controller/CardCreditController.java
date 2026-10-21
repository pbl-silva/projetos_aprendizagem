
package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.CUSTOMER_ID;

import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditAcceptanceDto;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditApplicationDto;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditOverviewDto;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditResultDto;
import br.com.spbank.cartao.application.port.in.CardCreditUseCase;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cards/credit")
public final class CardCreditController {

    private final CardCreditUseCase credit;

    public CardCreditController(
            CardCreditUseCase credit
    ) {
        this.credit = credit;
    }

    @GetMapping
    public CreditOverviewDto overview(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId
    ) {

        return CreditOverviewDto.from(
                credit.overview(customerId)
        );
    }

    @PostMapping("/requests")
    public CreditResultDto request(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @RequestHeader("Idempotency-Key")
            UUID key,

            @Valid
            @RequestBody
            CreditApplicationDto body
    ) {

        return CreditResultDto.from(
                credit.request(
                        customerId,
                        key,
                        body.requestedTotal(),
                        body.reason()
                )
        );
    }

    @PostMapping("/requests/{requestId}/accept")
    public CreditResultDto accept(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @PathVariable
            UUID requestId,

            @RequestHeader("Idempotency-Key")
            UUID key,

            @Valid
            @RequestBody
            CreditAcceptanceDto body
    ) {

        return CreditResultDto.from(
                credit.accept(
                        customerId,
                        requestId,
                        key,
                        body.confirmationPassword()
                )
        );
    }

    @PostMapping("/requests/{requestId}/cancel")
    public CreditResultDto cancel(
            @RequestAttribute(CUSTOMER_ID)
            UUID customerId,

            @PathVariable
            UUID requestId,

            @RequestHeader("Idempotency-Key")
            UUID key
    ) {

        return CreditResultDto.from(
                credit.cancel(
                        customerId,
                        requestId,
                        key
                )
        );
    }
}
