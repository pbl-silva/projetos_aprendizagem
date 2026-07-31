package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.util.TarifaCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TarifaService {

    public BigDecimal calcularValorBase(TipoVeiculo tipoVeiculo, LocalDateTime entrada,
                                        LocalDateTime saida) {
        long horas = TarifaCalculator.calcularHoras(entrada, saida);
        return TarifaCalculator.calcularValorBase(tipoVeiculo.getPrecoDiaria(), horas);
    }

    public BigDecimal calcularValor(TipoVeiculo tipoVeiculo, LocalDateTime entrada, 
                                    LocalDateTime saida, Modalidade modalidade) {
        BigDecimal valorBase = calcularValorBase(tipoVeiculo, entrada, saida);
        BigDecimal desconto = calcularDesconto(valorBase, modalidade);
        return valorBase.subtract(desconto);
    }
    
    public BigDecimal calcularDesconto(BigDecimal valorBase, Modalidade modalidade) {
        return TarifaCalculator.calcularDesconto(valorBase, modalidade.getDesconto());
    }
    
    public boolean podeEstacionarEmVaga(TipoVeiculo tipoVeiculo, boolean pcd, String tipoVaga) {
        if (tipoVeiculo == TipoVeiculo.MOTO) {
            return tipoVaga.equals("MOTO");
        } else if (tipoVeiculo == TipoVeiculo.CARRO_ELETRICO) {
            return tipoVaga.equals("ELETRICA") || tipoVaga.equals("COMUM");
        } else if (pcd) {
            return tipoVaga.equals("PCD") || tipoVaga.equals("COMUM");
        } else {
            return tipoVaga.equals("COMUM");
        }
    }
}
