package br.com.spbank.pix.adapter.out.persistence.mysql.converter;

import br.com.spbank.pix.application.model.PixRouting;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class PixRoutingConverter
        implements AttributeConverter<PixRouting, String> {

    @Override
    public String convertToDatabaseColumn(
        PixRouting value
    ) {
        if (value == null) {
            return null;
        }

        return value == PixRouting.INTERNAL
            ? "INTERNO"
            : "EXTERNO";
    }

    @Override
    public PixRouting convertToEntityAttribute(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "INTERNO" ->
                PixRouting.INTERNAL;

            case "EXTERNO" ->
                PixRouting.EXTERNAL;

            default ->
                throw new IllegalArgumentException(
                    "Escopo Pix inválido"
                );
        };
    }
}