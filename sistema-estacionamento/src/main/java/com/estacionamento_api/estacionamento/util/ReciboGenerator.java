package com.estacionamento_api.estacionamento.util;

import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.model.Entrada;
import com.estacionamento_api.estacionamento.model.Saida;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Utilitário puro para montar o recibo a partir de uma Saida já persistida.
 * Não depende do Spring, nem faz consulta a repositório — recebe tudo
 * que precisa como parâmetro.
 */
public final class ReciboGenerator {

    private ReciboGenerator() {
        // classe utilitária, não deve ser instanciada
    }

    public static String gerarNumeroRecibo() {
        return "REC-" + UUID.randomUUID().toString().toUpperCase();
    }

    public static ReciboDTO gerar(Saida saida, BigDecimal valorBase) {
        Entrada entrada = saida.getEntrada();
        long minutos = ChronoUnit.MINUTES.between(
            entrada.getDataHoraEntrada(),
            saida.getDataHoraSaida()
        );

        return ReciboDTO.builder()
            .id(saida.getId())
            .numeroRecibo(saida.getNumeroRecibo())
            .placa(entrada.getVeiculo().getPlaca())
            .marca(entrada.getVeiculo().getMarca())
            .modelo(entrada.getVeiculo().getModelo())
            .numeroVaga(entrada.getVaga().getNumero())
            .dataHoraEntrada(entrada.getDataHoraEntrada())
            .dataHoraSaida(saida.getDataHoraSaida())
            .tempoEstacionadoMinutos(minutos)
            .valorBase(valorBase)
            .desconto(saida.getDesconto())
            .valorFinal(saida.getValorPago())
            .tipoPagamento(saida.getTipoPagamento().getDescricao())
            .modalidade(saida.getModalidade().getDescricao())
            .dataEmissao(saida.getDataCriacao())
            .build();
    }
}
