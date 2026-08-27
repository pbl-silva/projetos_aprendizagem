package br.com.spbank.transferencia.application.port.in;

import br.com.spbank.conta.application.port.in.AccountLookup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransferCommand(
        UUID sourceAccountId,
        UUID idempotencyKey,
        String recipientName,
        String recipientDocument,
        AccountLookup target,
        BigDecimal amount,
        LocalDate scheduledFor
) {
}