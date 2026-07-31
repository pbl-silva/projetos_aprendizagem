package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import com.estacionamento_api.estacionamento.enums.TipoVaga;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VagaDTO {
    private Long id;
    private String numero;
    private TipoVaga tipoVaga;
    private Boolean disponivel;
}