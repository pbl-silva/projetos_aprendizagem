package br.com.spbank.conta.adapter.out.persistence.mysql.converter;

import br.com.spbank.conta.application.model.AccountType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class AccountTypeConverter
        implements AttributeConverter<AccountType, String> {

    @Override
    public String convertToDatabaseColumn(AccountType value) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case CURRENT -> "CORRENTE";
            case SAVINGS -> "POUPANCA";
        };
    }

    @Override
    public AccountType convertToEntityAttribute(String value) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "CORRENTE" -> AccountType.CURRENT;
            case "POUPANCA" -> AccountType.SAVINGS;

            default -> throw new IllegalArgumentException(
                    "Tipo de conta inválido: " + value
            );
        };
    }
}