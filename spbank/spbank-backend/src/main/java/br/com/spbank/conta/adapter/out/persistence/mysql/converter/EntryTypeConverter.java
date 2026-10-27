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
            case PIX_OUT -> "PIX_SAIDA";
            case PIX_IN -> "PIX_ENTRADA";
            case CARD_PAYMENT -> "PAGAMENTO_CARTAO";
            case INVESTMENT_APPLICATION -> "INVESTIMENTO_APLICACAO";
            case INVESTMENT_REDEMPTION -> "INVESTIMENTO_RESGATE";
            case PAYMENT_OUT -> "PAGAMENTO_SAIDA";
            case MOBILE_RECHARGE_OUT -> "RECARGA_CELULAR_SAIDA";
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
            case "PIX_SAIDA" -> EntryType.PIX_OUT;
            case "PIX_ENTRADA" -> EntryType.PIX_IN;
            case "PAGAMENTO_CARTAO" -> EntryType.CARD_PAYMENT;
            case "INVESTIMENTO_APLICACAO" -> EntryType.INVESTMENT_APPLICATION;
            case "INVESTIMENTO_RESGATE" -> EntryType.INVESTMENT_REDEMPTION;
            case "PAGAMENTO_SAIDA" -> EntryType.PAYMENT_OUT;
            case "RECARGA_CELULAR_SAIDA" -> EntryType.MOBILE_RECHARGE_OUT;
            case "TAXA" -> EntryType.FEE;

            default -> throw new IllegalArgumentException(
                    "Tipo de lançamento inválido: " + value
            );
        };
    }
}