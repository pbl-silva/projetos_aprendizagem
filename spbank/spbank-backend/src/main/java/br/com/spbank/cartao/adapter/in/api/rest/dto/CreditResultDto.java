
package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CreditResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditResultDto(

        UUID operationId,
        UUID actorId,
        String action,
        CreditRequestDto request,
        BigDecimal approvedTotal,
        Instant occurredAt

) {

    public static CreditResultDto from(
            CreditResult result
    ) {

        return new CreditResultDto(
                result.operationId(),
                result.actorId(),
                result.action(),
                CreditRequestDto.from(
                        result.request()
                ),
                result.approvedTotal(),
                result.occurredAt()
        );
    }
}
