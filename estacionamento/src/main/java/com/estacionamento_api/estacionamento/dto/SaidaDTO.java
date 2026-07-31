package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoPagamento;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaidaDTO {
    private Long id;
    private LocalDateTime dataHoraSaida;
    private Long entradaId;
    private BigDecimal valorPago;
    private TipoPagamento tipoPagamento;
    private Modalidade modalidade;
    private BigDecimal desconto;
}