package br.com.spbank.conta.adapter.out.persistence.mysql.converter;

import br.com.spbank.conta.application.model.EntryType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class EntryTypeConverter
        implements AttributeConverter<EntryType, String> {

    @Override
    public String convertToDatabaseColumn(EntryType value) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case TRANSFER_OUT -> "TRANSFERENCIA_SAIDA";
            case TRANSFER_IN -> "TRANSFERENCIA_ENTRADA";
            case FEE -> "TAXA";
        };
    }

    @Override
    public EntryType convertToEntityAttribute(String value) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "TRANSFERENCIA_SAIDA" -> EntryType.TRANSFER_OUT;
            case "TRANSFERENCIA_ENTRADA" -> EntryType.TRANSFER_IN;
            case "TAXA" -> EntryType.FEE;

            default -> throw new IllegalArgumentException(
                    "Tipo de lançamento inválido: " + value
            );
        };
    }
}