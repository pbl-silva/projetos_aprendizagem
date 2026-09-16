package br.com.spbank.pix.adapter.out.persistence.mysql.converter;

import br.com.spbank.pix.application.model.PixKeyType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class PixKeyTypeConverter
        implements AttributeConverter<PixKeyType, String> {

    @Override
    public String convertToDatabaseColumn(
        PixKeyType value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case CPF_CNPJ -> "CPF_CNPJ";
            case EMAIL -> "EMAIL";
            case PHONE -> "CELULAR";
            case RANDOM -> "ALEATORIA";
        };
    }

    @Override
    public PixKeyType convertToEntityAttribute(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "CPF_CNPJ" -> PixKeyType.CPF_CNPJ;
            case "EMAIL" -> PixKeyType.EMAIL;
            case "CELULAR" -> PixKeyType.PHONE;
            case "ALEATORIA" -> PixKeyType.RANDOM;

            default -> throw new IllegalArgumentException(
                "Tipo de chave PIX inválido: " + value
            );
        };
    }
}