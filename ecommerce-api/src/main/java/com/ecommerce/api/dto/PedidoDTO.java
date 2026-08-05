package com.ecommerce.api.dto;

import com.ecommerce.api.enums.StatusPedido;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long id;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;

    @DecimalMin(value = "0.0", message = "Total não pode ser negativo")
    private Double total;

    private StatusPedido status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}