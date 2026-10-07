package br.com.spbank.pix.adapter.in.api.rest.dto;

import br.com.spbank.pix.application.model.PixFrequency;
import br.com.spbank.pix.application.model.PixKeyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PixPreviewDto(
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