package br.com.spbank.transferencia.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.transferencia.application.model.transfer.TransferStatus;
import br.com.spbank.transferencia.application.model.transfer.TransferType;

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