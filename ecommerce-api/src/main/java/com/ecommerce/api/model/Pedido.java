package com.ecommerce.api.model;

import com.ecommerce.api.enums.StatusPedido;
import com.ecommerce.api.enums.ModalidadeFrete;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private Double total;

    @Builder.Default
    @Column(nullable = false)
    private Double subtotal = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double frete = 0.0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalidadeFrete modalidadeFrete = ModalidadeFrete.RETIRADA;

    @Column(length = 8)
    private String cepEntrega;

    @Column(length = 255)
    private String enderecoEntrega;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status = StatusPedido.PENDENTE;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column
    private LocalDateTime dataAtualizacao;

    @Builder.Default
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();
}
