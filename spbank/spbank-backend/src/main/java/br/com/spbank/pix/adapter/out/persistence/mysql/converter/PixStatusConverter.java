package br.com.spbank.pix.adapter.out.persistence.mysql.converter;

import br.com.spbank.pix.application.model.PixStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class PixStatusConverter
        implements AttributeConverter<PixStatus, String> {

    @Override
    public String convertToDatabaseColumn(
        PixStatus value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case SCHEDULED -> "AGENDADO";
            case PROCESSING -> "PROCESSANDO";
            case COMPLETED -> "CONCLUIDO";
            case FAILED -> "FALHA";
            case CANCELLED -> "CANCELADO";
        };
    }

    @Override
    public PixStatus convertToEntityAttribute(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "AGENDADO" -> PixStatus.SCHEDULED;
            case "PROCESSANDO" -> PixStatus.PROCESSING;
            case "CONCLUIDO" -> PixStatus.COMPLETED;
            case "FALHA" -> PixStatus.FAILED;
            case "CANCELADO" -> PixStatus.CANCELLED;

            default -> throw new IllegalArgumentException(
                "Situação de PIX inválida: " + value
            );
        };
    }
}