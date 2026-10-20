package com.ecommerce.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedidoDTO {
    private Long id;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    private BigDecimal precoUnitario;

    @NotNull(message = "Produto é obrigatório")
    private Long produtoId;

    private String produtoNome;
    private BigDecimal subtotal;
}
