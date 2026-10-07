package br.com.spbank.pix.adapter.out.persistence.mysql.converter;

import br.com.spbank.pix.application.model.PixFrequency;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class PixFrequencyConverter
        implements AttributeConverter<PixFrequency, String> {

    @Override
    public String convertToDatabaseColumn(
        PixFrequency value
    ) {
        if (value == null) {
            return null;
        }

        return value == PixFrequency.WEEKLY
            ? "SEMANAL"
            : "MENSAL";
    }

    @Override
    public PixFrequency convertToEntityAttribute(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "SEMANAL" ->
                PixFrequency.WEEKLY;

            case "MENSAL" ->
                PixFrequency.MONTHLY;

            default ->
                throw new IllegalArgumentException(
                    "Frequência Pix inválida"
                );
        };
    }
}