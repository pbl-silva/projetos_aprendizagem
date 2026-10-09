package br.com.spbank.administracao.adapter.out.persistence.mysql.converter;

import br.com.spbank.shared.application.model.CustomerPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class AdministrativeCustomerPlanConverter
        implements AttributeConverter<CustomerPlan, String> {

    @Override
    public String convertToDatabaseColumn(
            CustomerPlan value
    ) {

        return value == null
                ? null
                : value.name();
    }

    @Override
    public CustomerPlan convertToEntityAttribute(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return switch (value) {
            case "STANDARD" -> CustomerPlan.STANDARD;
            case "PLUS" -> CustomerPlan.PLUS;

            default -> throw new IllegalArgumentException(
                    "Plano de cliente inválido: " + value
            );
        };
    }
}