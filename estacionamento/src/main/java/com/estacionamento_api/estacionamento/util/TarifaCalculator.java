package com.estacionamento_api.estacionamento.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Utilitário puro para os cálculos monetários de tarifa.
 * Não depende do Spring (sem estado, sem I/O) — pode ser testado
 * isoladamente sem subir contexto nem usar mocks.
 */
public final class TarifaCalculator {

    private TarifaCalculator() {
        // classe utilitária, não deve ser instanciada
    }

    /**
     * Calcula as horas de permanência arredondadas para cima, com cobrança
     * mínima de 1 hora. Sem o mínimo, um veículo que entra e sai no mesmo
     * minuto (0 minutos de permanência) seria cobrado R$ 0,00, já que
     * (0 + 59) / 60 = 0 na divisão inteira.
     */
    public static long calcularHoras(LocalDateTime entrada, LocalDateTime saida) {
        long minutos = ChronoUnit.MINUTES.between(entrada, saida);
        if (minutos <= 0) {
            return 1;
        }
        return (minutos + 59) / 60;
    }

    /**
     * Valor bruto (sem desconto de modalidade) para o tempo estacionado.
     * Usa BigDecimal.valueOf(double) para evitar o erro de arredondamento
     * binário de "new BigDecimal(double)".
     */
    public static BigDecimal calcularValorBase(double precoDiaria, long horas) {
        return BigDecimal.valueOf(precoDiaria)
            .multiply(BigDecimal.valueOf(horas))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valor do desconto (em reais) dado um percentual (ex: 0.15 = 15%).
     */
    public static BigDecimal calcularDesconto(BigDecimal valorBase, double percentualDesconto) {
        return valorBase.multiply(BigDecimal.valueOf(percentualDesconto))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
