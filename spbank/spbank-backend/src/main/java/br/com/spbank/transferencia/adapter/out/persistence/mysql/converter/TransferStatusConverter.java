package br.com.spbank.transferencia.adapter.out.persistence.mysql.converter;

import br.com.spbank.transferencia.application.model.TransferStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class TransferStatusConverter
        implements AttributeConverter<TransferStatus, String> {

    @Override
    public String convertToDatabaseColumn(
            TransferStatus value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case SCHEDULED -> "AGENDADA";
            case PROCESSING -> "PROCESSANDO";
            case COMPLETED -> "CONCLUIDA";
            case FAILED -> "FALHA";
            case CANCELLED -> "CANCELADA";
        };
    }

    @Override
    public TransferStatus convertToEntityAttribute(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "AGENDADA" -> TransferStatus.SCHEDULED;
            case "PROCESSANDO" -> TransferStatus.PROCESSING;
            case "CONCLUIDA" -> TransferStatus.COMPLETED;
            case "FALHA" -> TransferStatus.FAILED;
            case "CANCELADA" -> TransferStatus.CANCELLED;

            default -> throw new IllegalArgumentException(
                    "Situação de transferência inválida: " + value
            );
        };
    }
}