package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReciboDTO {
    private Long id;
    private String numeroRecibo;
    private String placa;
    private String marca;
    private String modelo;
    private String numeroVaga;
    private LocalDateTime dataHoraEntrada;
    private LocalDateTime dataHoraSaida;
    private Long tempoEstacionadoMinutos;
    private BigDecimal valorBase;
    private BigDecimal desconto;
    private BigDecimal valorFinal;
    private String tipoPagamento;
    private String modalidade;
    private LocalDateTime dataEmissao;
}