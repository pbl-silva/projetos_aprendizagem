package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntradaDTO {
    private Long id;
    private LocalDateTime dataHoraEntrada;

    @NotNull(message = "O ID do veículo é obrigatório")
    private Long veiculoId;

    private String placaVeiculo;
    private Long vagaId;
    private String numeroVaga;
    private Boolean ativo;
}