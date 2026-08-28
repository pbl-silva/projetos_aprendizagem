package br.com.spbank.conta.adapter.out.persistence.mysql.converter;

import br.com.spbank.conta.application.model.EntryReferenceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class EntryReferenceTypeConverter
        implements AttributeConverter<EntryReferenceType, String> {

    @Override
    public String convertToDatabaseColumn(
            EntryReferenceType value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case TRANSFER -> "TRANSFERENCIA";
            case PIX -> "PIX";
            case CARD -> "CARTAO";
            case INVESTMENT -> "INVESTIMENTO";
        };
    }

    @Override
    public EntryReferenceType convertToEntityAttribute(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "TRANSFERENCIA" -> EntryReferenceType.TRANSFER;
            case "PIX" -> EntryReferenceType.PIX;
            case "CARTAO" -> EntryReferenceType.CARD;
            case "INVESTIMENTO" -> EntryReferenceType.INVESTMENT;

            default -> throw new IllegalArgumentException(
                    "Tipo de referência inválido: " + value
            );
        };
    }
}