package com.ecommerce.api.dto;

import com.ecommerce.api.enums.StatusPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long id;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;

    private Double total;

    private StatusPedido status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    @Valid
    @NotEmpty(message = "O pedido deve conter pelo menos um produto")
    private List<ItemPedidoDTO> itens;
}
