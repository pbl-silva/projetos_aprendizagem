
package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CreditRequest;
import br.com.spbank.cartao.application.model.CreditRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditRequestDto(

        UUID id,
        UUID customerId,
        BigDecimal requestedTotal,
        String reason,
        CreditRequestStatus status,
        BigDecimal offeredTotal,
        UUID administratorId,
        String decisionReason,
        Instant createdAt,
        Instant decidedAt,
        Instant expiresAt,
        Instant closedAt

) {

    public static CreditRequestDto from(
            CreditRequest request
    ) {

        return new CreditRequestDto(
                request.id(),
                request.customerId(),
                request.requestedTotal(),
                request.reason(),
                request.status(),
                request.offeredTotal(),
                request.administratorId(),
                request.decisionReason(),
                request.createdAt(),
                request.decidedAt(),
                request.expiresAt(),
                request.closedAt()
        );
    }
}
