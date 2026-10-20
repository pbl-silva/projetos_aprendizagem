package com.estacionamento_api.estacionamento.dto;

import lombok.*;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaidaDTO {
    @NotNull(message = "O ID da entrada é obrigatório")
    @Positive(message = "O ID da entrada deve ser positivo")
    private Long entradaId;

    @NotNull(message = "O tipo de pagamento é obrigatório")
    private TipoPagamento tipoPagamento;

    @NotNull(message = "A modalidade é obrigatória")
    private Modalidade modalidade;

}
