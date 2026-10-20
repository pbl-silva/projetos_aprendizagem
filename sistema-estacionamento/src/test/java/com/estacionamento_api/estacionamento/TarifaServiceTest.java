package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.service.TarifaService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TarifaServiceTest {
    private final TarifaService tarifaService = new TarifaService();

    @Test
    void deveCobrarDuasHorasDeCarro() {
        LocalDateTime entrada = LocalDateTime.of(2026, 1, 1, 10, 0);
        BigDecimal valor = tarifaService.calcularValorBase(TipoVeiculo.CARRO, entrada, entrada.plusHours(2));
        assertEquals(0, valor.compareTo(new BigDecimal("40.00")));
    }

    @Test
    void deveCalcularDescontoMensal() {
        BigDecimal desconto = tarifaService.calcularDesconto(new BigDecimal("20.00"), Modalidade.MENSAL);
        assertEquals(0, desconto.compareTo(new BigDecimal("3.00")));
    }
}
