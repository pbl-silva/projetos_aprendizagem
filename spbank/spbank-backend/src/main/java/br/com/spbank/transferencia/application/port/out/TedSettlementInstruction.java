package br.com.spbank.transferencia.application.port.out;

import br.com.spbank.transferencia.application.modelRecipient;

import java.math.BigDecimal;
import java.util.UUID;

public record TedSettlementInstruction(
        UUID transferId,
        UUID sourceAccountId,
        TransferRecipient recipient,
        BigDecimal amount
) {
}