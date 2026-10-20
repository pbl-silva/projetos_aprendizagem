package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.util.TarifaCalculator;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TarifaService {

    public BigDecimal calcularValorBase(TipoVeiculo tipoVeiculo, LocalDateTime entrada,
                                        LocalDateTime saida) {
        long horas = TarifaCalculator.calcularHoras(entrada, saida);
        return TarifaCalculator.calcularValorBase(tipoVeiculo.getPrecoHora(), horas);
    }

    public BigDecimal calcularDesconto(BigDecimal valorBase, Modalidade modalidade) {
        return TarifaCalculator.calcularDesconto(valorBase, modalidade.getDesconto());
    }
    
}
