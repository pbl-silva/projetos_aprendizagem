package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntradaDTO {
    private Long id;
    private LocalDateTime dataHoraEntrada;
    private Long veiculoId;
    private String placaVeiculo;
    private Long vagaId;
    private String numeroVaga;
    private Boolean ativo;
}