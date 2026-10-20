package com.biblioteca.biblioteca_api.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("PREMIUM")
public class CalculadoraMultaPremium implements CalculadoraMulta {
    private static final BigDecimal VALOR_POR_DIA = new BigDecimal("2.00");

    /**
     * Recebe a quantidade de dias já multáveis (a tolerância é aplicada uma
     * única vez, em EmprestimoServiceImpl.calcularMulta()), portanto aqui
     * apenas multiplicamos pelo valor diário — sem descontar tolerância de novo.
     */
    @Override
    public BigDecimal calcular(long diasDeAtraso) {
        if (diasDeAtraso <= 0) return BigDecimal.ZERO;
        return VALOR_POR_DIA.multiply(BigDecimal.valueOf(diasDeAtraso));
    }
}