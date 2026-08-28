package br.com.spbank.conta.adapter.out.persistence.mysql.converter;

import br.com.spbank.conta.application.model.EntryDirection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class EntryDirectionConverter
        implements AttributeConverter<EntryDirection, String> {

    @Override
    public String convertToDatabaseColumn(EntryDirection value) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case DEBIT -> "DEBITO";
            case CREDIT -> "CREDITO";
        };
    }

    @Override
    public EntryDirection convertToEntityAttribute(String value) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "DEBITO" -> EntryDirection.DEBIT;
            case "CREDITO" -> EntryDirection.CREDIT;

            default -> throw new IllegalArgumentException(
                    "Natureza do lançamento inválida: " + value
            );
        };
    }
}