package br.com.spbank.autenticacao.adapter.out.persistence.mysql.converter;

import br.com.spbank.shared.application.model.CustomerPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class CustomerPlanConverter
        implements AttributeConverter<CustomerPlan, String> {

    @Override
    public String convertToDatabaseColumn(CustomerPlan value) {
        return value == null
                ? null
                : value.name();
    }

    @Override
    public CustomerPlan convertToEntityAttribute(String value) {
        return value == null
                ? null
                : CustomerPlan.valueOf(value);
    }
}