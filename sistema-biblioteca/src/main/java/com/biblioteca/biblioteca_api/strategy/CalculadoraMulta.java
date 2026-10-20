package com.biblioteca.biblioteca_api.strategy;

import java.math.BigDecimal;

public interface CalculadoraMulta {
    BigDecimal calcular(long diasDeAtraso);
}