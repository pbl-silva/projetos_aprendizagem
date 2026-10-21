
package br.com.spbank.cartao.adapter.in.api.rest.controller;

import static br.com.spbank.shared.adapter.in.api.rest.security.AuthenticatedRequest.ADMINISTRATOR_ID;

import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditDecisionDto;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditRequestDto;
import br.com.spbank.cartao.adapter.in.api.rest.dto.CreditResultDto;
import br.com.spbank.cartao.application.port.in.CardCreditUseCase;

import jakarta.validation.Valid;

import java.util.List;
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
@RequestMapping("/api/v1/admin/card-credit")
public final class AdministrativeCardCreditController {

    private final CardCreditUseCase credit;

    public AdministrativeCardCreditController(
            CardCreditUseCase credit
    ) {
        this.credit = credit;
    }

    @GetMapping("/requests")
    public List<CreditRequestDto> pending(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId
    ) {

        return credit
                .pending(administratorId)
                .stream()
                .map(CreditRequestDto::from)
                .toList();
    }

    @PostMapping(
            "/customers/{customerId}/requests/{requestId}/decision"
    )
    public CreditResultDto decide(
            @RequestAttribute(ADMINISTRATOR_ID)
            UUID administratorId,

            @PathVariable
            UUID customerId,

            @PathVariable
            UUID requestId,

            @RequestHeader("Idempotency-Key")
            UUID key,

            @Valid
            @RequestBody
            CreditDecisionDto body
    ) {

        return CreditResultDto.from(
                credit.decide(
                        administratorId,
                        customerId,
                        requestId,
                        key,
                        body.decision(),
                        body.offeredTotal(),
                        body.reason()
                )
        );
    }
}