package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.service.TarifaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes TarifaService")
class TarifaServiceTest {
    
    @InjectMocks
    private TarifaService tarifaService;
    
    @Test
    @DisplayName("Deve calcular tarifa de carro corretamente")
    void testCalcularTarifaCarro() {
        LocalDateTime entrada = LocalDateTime.now().minusHours(2);
        LocalDateTime saida = LocalDateTime.now();
        
        BigDecimal valor = tarifaService.calcularValor(
            TipoVeiculo.CARRO,
            entrada,
            saida,
            Modalidade.DIARIA
        );
        
        BigDecimal esperado = new BigDecimal("40.00");
        assertEquals(0, valor.compareTo(esperado));
    }
    
    @Test
    @DisplayName("Deve aplicar desconto mensal corretamente")
    void testCalcularComDescontoMensal() {
        LocalDateTime entrada = LocalDateTime.now().minusHours(1);
        LocalDateTime saida = LocalDateTime.now();
        
        BigDecimal valor = tarifaService.calcularValor(
            TipoVeiculo.CARRO,
            entrada,
            saida,
            Modalidade.MENSAL
        );
        
        // 20 * 0.85 = 17
        BigDecimal esperado = new BigDecimal("17.00");
        assertEquals(0, valor.compareTo(esperado));
    }
    
    @Test
    @DisplayName("Deve validar compatibilidade de vaga para moto")
    void testValidarCompatibilidadeMoto() {
        assertTrue(tarifaService.podeEstacionarEmVaga(
            TipoVeiculo.MOTO, false, "MOTO"));
        assertFalse(tarifaService.podeEstacionarEmVaga(
            TipoVeiculo.MOTO, false, "COMUM"));
    }

    @Test
    @DisplayName("Carro comum só pode estacionar em vaga comum")
    void testCarroComumNaoUsaVagaPcd() {
        assertTrue(tarifaService.podeEstacionarEmVaga(
            TipoVeiculo.CARRO, false, "COMUM"));
        assertFalse(tarifaService.podeEstacionarEmVaga(
            TipoVeiculo.CARRO, false, "PCD"));
    }

    @Test
    @DisplayName("Carro PCD pode estacionar em vaga PCD ou comum")
    void testCarroPcdUsaVagaPcdOuComum() {
        assertTrue(tarifaService.podeEstacionarEmVaga(
            TipoVeiculo.CARRO, true, "PCD"));
        assertTrue(tarifaService.podeEstacionarEmVaga(
            TipoVeiculo.CARRO, true, "COMUM"));
    }
}
  