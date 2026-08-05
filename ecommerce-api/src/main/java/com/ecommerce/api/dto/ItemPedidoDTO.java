package com.ecommerce.api.dto;

import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedidoDTO {
    private Long id;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private Long produtoId;
    private String produtoNome;
    private BigDecimal subtotal;
}