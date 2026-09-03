package br.com.spbank.administracao.adapter.out.persistence.mysql.converter;

import br.com.spbank.conta.application.model.AccountPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class AdministrativeAccountPlanConverter
        implements AttributeConverter<AccountPlan, String> {

    @Override
    public String convertToDatabaseColumn(
            AccountPlan value
    ) {

        return value == null
                ? null
                : value.name();
    }

    @Override
    public AccountPlan convertToEntityAttribute(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "STANDARD" ->
                    AccountPlan.STANDARD;

            case "PLUS" ->
                    AccountPlan.PLUS;

            default ->
                    throw new IllegalArgumentException(
                            "Plano de conta inválido: "
                                    + value
                    );
        };
    }
}