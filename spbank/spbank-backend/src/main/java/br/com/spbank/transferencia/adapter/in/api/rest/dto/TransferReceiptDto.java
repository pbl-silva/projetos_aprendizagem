package br.com.spbank.transferencia.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.transferencia.application.modelStatus;
import br.com.spbank.transferencia.application.modelType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransferReceiptDto(
        UUID id,
        TransferType type,
        TransferStatus status,
        BigDecimal amount,
        BigDecimal fee,
        String recipientName,
        String bankCode,
        String branch,
        String accountNumber,
        AccountType accountType,
        LocalDate scheduledFor,
        Instant requestedAt,
        Instant processedAt,
        String failureCode,
        String settlementReference
) {
}