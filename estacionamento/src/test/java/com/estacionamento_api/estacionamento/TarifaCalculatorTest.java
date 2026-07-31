package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.util.TarifaCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes TarifaCalculator")
class TarifaCalculatorTest {

    @Test
    @DisplayName("Deve arredondar horas para cima")
    void testCalcularHorasArredondaParaCima() {
        LocalDateTime entrada = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime saida = LocalDateTime.of(2026, 1, 1, 11, 1); // 61 minutos

        assertEquals(2, TarifaCalculator.calcularHoras(entrada, saida));
    }

    @Test
    @DisplayName("Deve calcular horas exatas sem arredondar para mais")
    void testCalcularHorasExatas() {
        LocalDateTime entrada = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime saida = LocalDateTime.of(2026, 1, 1, 12, 0); // 120 minutos

        assertEquals(2, TarifaCalculator.calcularHoras(entrada, saida));
    }

    @Test
    @DisplayName("Deve cobrar o mínimo de 1 hora mesmo com permanência de 0 minutos")
    void testCalcularHorasComPermanenciaMinima() {
        LocalDateTime entrada = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime saida = LocalDateTime.of(2026, 1, 1, 10, 0); // 0 minutos

        // Sem o mínimo, (0 + 59) / 60 = 0 na divisão inteira e o cliente
        // sairia sem pagar nada.
        assertEquals(1, TarifaCalculator.calcularHoras(entrada, saida));
    }

    @Test
    @DisplayName("Deve calcular valor base multiplicando preço da diária pelas horas")
    void testCalcularValorBase() {
        BigDecimal valor = TarifaCalculator.calcularValorBase(20.00, 2);
        assertEquals(0, valor.compareTo(new BigDecimal("40.00")));
    }

    @Test
    @DisplayName("Deve calcular desconto sem erro de arredondamento binário")
    void testCalcularDescontoSemErroDeArredondamento() {
        BigDecimal valorBase = new BigDecimal("20.00");
        BigDecimal desconto = TarifaCalculator.calcularDesconto(valorBase, 0.15);

        // Com new BigDecimal(double) isso dava 2.9999999999999998... em vez de 3.00
        assertEquals(0, desconto.compareTo(new BigDecimal("3.00")));
    }
}
