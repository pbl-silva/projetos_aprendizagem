package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeiculoDTO {
    private Long id;
    private String placa;
    private TipoVeiculo tipoVeiculo;
    private String marca;
    private String modelo;
    private String cor;
    private Boolean pcd;
}