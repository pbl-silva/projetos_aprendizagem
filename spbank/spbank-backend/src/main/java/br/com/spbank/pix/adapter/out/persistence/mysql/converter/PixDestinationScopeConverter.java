package br.com.spbank.pix.adapter.out.persistence.mysql.converter;

import br.com.spbank.pix.application.model.PixDestinationScope;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class PixDestinationScopeConverter
        implements AttributeConverter<PixDestinationScope, String> {

    @Override
    public String convertToDatabaseColumn(
        PixDestinationScope value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case INTERNAL -> "INTERNO";
            case EXTERNAL -> "EXTERNO";
        };
    }

    @Override
    public PixDestinationScope convertToEntityAttribute(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "INTERNO" ->
                PixDestinationScope.INTERNAL;

            case "EXTERNO" ->
                PixDestinationScope.EXTERNAL;

            default -> throw new IllegalArgumentException(
                "Escopo de destino PIX inválido: "
                    + value
            );
        };
    }
}