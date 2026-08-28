package br.com.spbank.transferencia.application.service.transfer;

import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.transferencia.application.model.transfer.TransferType;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public final class TransferPolicy {

    private final TransferProperties properties;

    public TransferPolicy(TransferProperties properties) {
        this.properties = properties;
    }

    public void requireWithinLimit(
            TransferType type,
            BigDecimal amount
    ) {

        BigDecimal limit = maxPerOperation(type);

        if (amount.compareTo(limit) > 0) {
            throw new BusinessException(
                    "TRANSFER_LIMIT_EXCEEDED",
                    "transfer.limit-exceeded"
            );
        }
    }

    private BigDecimal maxPerOperation(TransferType type) {

        return type == TransferType.INTERNAL
                ? properties.internal().maxPerOperation()
                : properties.ted().maxPerOperation();
    }
}