package com.estacionamento_api.estacionamento.model;

import jakarta.persistence.*;
import lombok.*;
import com.estacionamento_api.estacionamento.enums.Modalidade;
import com.estacionamento_api.estacionamento.enums.TipoPagamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saidas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Saida {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_recibo", nullable = false, unique = true, length = 40)
    private String numeroRecibo;
    
    @Column(name = "data_hora_saida", nullable = false)
    private LocalDateTime dataHoraSaida;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id", nullable = false, unique = true)
    private Entrada entrada;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPagamento tipoPagamento;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalidade modalidade;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal desconto;
    
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime agora = LocalDateTime.now();
        if (numeroRecibo == null) {
            numeroRecibo = "REC-" + UUID.randomUUID().toString().toUpperCase();
        }
        if (dataCriacao == null) {
            dataCriacao = agora;
        }
        if (dataHoraSaida == null) {
            dataHoraSaida = agora;
        }
    }
}
