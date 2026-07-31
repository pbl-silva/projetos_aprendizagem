package com.estacionamento_api.estacionamento.exception;

import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TarifaService {
    
    public BigDecimal calcularValor(TipoVeiculo tipoVeiculo, LocalDateTime entrada, 
                                    LocalDateTime saida, Modalidade modalidade) {
        long minutos = ChronoUnit.MINUTES.between(entrada, saida);
        long horas = (minutos + 59) / 60; // Arredonda para cima
        
        BigDecimal precoDiaria = new BigDecimal(tipoVeiculo.getPrecoDiaria());
        BigDecimal valorBase = precoDiaria.multiply(new BigDecimal(horas));
        
        // Aplicar desconto da modalidade
        BigDecimal desconto = valorBase.multiply(
            new BigDecimal(modalidade.getDesconto())
        );
        
        return valorBase.subtract(desconto);
    }
    
    public BigDecimal calcularDesconto(BigDecimal valorBase, Modalidade modalidade) {
        return valorBase.multiply(new BigDecimal(modalidade.getDesconto()));
    }
    
    public boolean podeEstacionarEmVaga(TipoVeiculo tipoVeiculo, String tipoVaga) {
        if (tipoVeiculo == TipoVeiculo.MOTO) {
            return tipoVaga.equals("MOTO");
        } else if (tipoVeiculo == TipoVeiculo.CARRO_ELETRICO) {
            return tipoVaga.equals("ELETRICA") || tipoVaga.equals("COMUM");
        } else {
            return tipoVaga.equals("COMUM") || tipoVaga.equals("PCD");
        }
    }
}