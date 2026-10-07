package br.com.spbank.pix.application.port.out;

import br.com.spbank.pix.application.model.PixKeyType;

import java.math.BigDecimal;
import java.util.UUID;

public record PixSettlementInstruction(
    UUID pixId,
    UUID sourceAccountId,
    PixKeyType keyType,
    String keyFingerprint,
    String destinationBankCode,
    BigDecimal amount
) {
}