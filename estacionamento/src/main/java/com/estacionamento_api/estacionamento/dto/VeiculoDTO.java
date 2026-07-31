package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeiculoDTO {
    private Long id;

    @NotBlank(message = "A placa é obrigatória")
    @Pattern(
        regexp = "^[A-Za-z]{3}-?\\d[A-Za-z0-9]\\d{2}$",
        message = "Placa em formato inválido (ex: ABC1234 ou ABC1D23)"
    )
    private String placa;

    @NotNull(message = "O tipo de veículo é obrigatório")
    private TipoVeiculo tipoVeiculo;

    @NotBlank(message = "A marca é obrigatória")
    @Size(max = 50, message = "A marca deve ter no máximo 50 caracteres")
    private String marca;

    @NotBlank(message = "O modelo é obrigatório")
    @Size(max = 50, message = "O modelo deve ter no máximo 50 caracteres")
    private String modelo;

    @Size(max = 30, message = "A cor deve ter no máximo 30 caracteres")
    private String cor;

    private Boolean pcd;
}