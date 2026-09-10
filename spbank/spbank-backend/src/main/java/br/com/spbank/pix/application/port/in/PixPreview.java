package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixRecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PixPreview(
    String recipientName,
    String institutionName,
    String maskedKey,
    String maskedDocument,
    BigDecimal amount,
    LocalDate scheduledFor,
    PixRecurrenceFrequency recurrenceFrequency,
    Integer totalOccurrences
) {

    @Override
    public String toString() {
        return "PixPreview[recipientName="
            + recipientName
            + ", institutionName="
            + institutionName
            + ", maskedKey="
            + maskedKey
            + ", maskedDocument="
            + maskedDocument
            + ", amount="
            + amount
            + ", scheduledFor="
            + scheduledFor
            + ", recurrenceFrequency="
            + recurrenceFrequency
            + ", totalOccurrences="
            + totalOccurrences
            + "]";
    }
}