package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixFrequency;
import br.com.spbank.pix.application.model.PixKeyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PixPreview(
    String recipientName,
    String maskedRecipientDocument,
    String destinationBankCode,
    String destinationBankName,
    PixKeyType keyType,
    String maskedKey,
    BigDecimal amount,
    LocalDate scheduledFor,
    PixFrequency frequency,
    Integer occurrences
) {
}