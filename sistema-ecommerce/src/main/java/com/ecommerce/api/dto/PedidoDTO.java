package com.ecommerce.api.dto;

import com.ecommerce.api.enums.StatusPedido;
import com.ecommerce.api.enums.ModalidadeFrete;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    private String clienteNome;

    private Double total;
    private Double subtotal;
    private Double frete;
    private ModalidadeFrete modalidadeFrete;

    @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos")
    private String cepEntrega;

    @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
    private String enderecoEntrega;

    private StatusPedido status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    @NotEmpty(message = "O pedido deve conter pelo menos um produto")
    private List<@Valid ItemPedidoDTO> itens;
}
