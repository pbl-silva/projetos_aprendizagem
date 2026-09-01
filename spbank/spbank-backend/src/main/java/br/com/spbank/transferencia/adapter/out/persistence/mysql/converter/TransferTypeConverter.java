package br.com.spbank.transferencia.adapter.out.persistence.mysql.converter;

import br.com.spbank.transferencia.application.modelType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class TransferTypeConverter
        implements AttributeConverter<TransferType, String> {

    @Override
    public String convertToDatabaseColumn(
            TransferType value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case INTERNAL -> "INTERNA";
            case TED -> "TED";
        };
    }

    @Override
    public TransferType convertToEntityAttribute(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "INTERNA" -> TransferType.INTERNAL;
            case "TED" -> TransferType.TED;

            default -> throw new IllegalArgumentException(
                    "Tipo de transferência inválido: " + value
            );
        };
    }
}