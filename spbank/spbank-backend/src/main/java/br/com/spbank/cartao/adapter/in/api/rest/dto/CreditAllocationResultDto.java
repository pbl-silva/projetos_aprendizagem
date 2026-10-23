package br.com.spbank.cartao.adapter.in.api.rest.dto;

import br.com.spbank.cartao.application.model.CreditAllocationResult;
import java.time.Instant;
import java.util.UUID;

public record CreditAllocationResultDto(
        UUID operationId, CreditAllocationSummaryDto credit, Instant occurredAt) {
    public static CreditAllocationResultDto from(CreditAllocationResult result) {
        return new CreditAllocationResultDto(result.operationId(),
            CreditAllocationSummaryDto.from(result.credit()), result.occurredAt());
    }
}
