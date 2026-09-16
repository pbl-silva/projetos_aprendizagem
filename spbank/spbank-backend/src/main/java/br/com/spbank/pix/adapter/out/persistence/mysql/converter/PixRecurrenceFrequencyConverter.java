package br.com.spbank.pix.adapter.out.persistence.mysql.converter;

import br.com.spbank.pix.application.model.PixRecurrenceFrequency;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class PixRecurrenceFrequencyConverter
        implements AttributeConverter<PixRecurrenceFrequency, String> {

    @Override
    public String convertToDatabaseColumn(
        PixRecurrenceFrequency value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case WEEKLY -> "SEMANAL";
            case MONTHLY -> "MENSAL";
        };
    }

    @Override
    public PixRecurrenceFrequency convertToEntityAttribute(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "SEMANAL" ->
                PixRecurrenceFrequency.WEEKLY;

            case "MENSAL" ->
                PixRecurrenceFrequency.MONTHLY;

            default -> throw new IllegalArgumentException(
                "Frequência de recorrência PIX inválida: "
                    + value
            );
        };
    }
}