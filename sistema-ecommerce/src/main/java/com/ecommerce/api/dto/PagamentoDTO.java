package com.ecommerce.api.dto;

import com.ecommerce.api.enums.StatusPagamento;
import com.ecommerce.api.enums.MetodoPagamento;
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
public class PagamentoDTO {
    private Long id;

    @NotNull(message = "Pedido é obrigatório")
    private Long pedidoId;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que 0")
    private Double valor;

    @NotNull(message = "Método de pagamento é obrigatório")
    private MetodoPagamento metodo;

    private StatusPagamento status;
    private LocalDateTime dataCriacao;
}
