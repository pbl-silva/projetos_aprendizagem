package com.estacionamento_api.estacionamento.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entradas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrada {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "data_hora_entrada", nullable = false)
    private LocalDateTime dataHoraEntrada;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vaga vaga;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
    
    @OneToOne(mappedBy = "entrada", cascade = CascadeType.ALL)
    private Saida saida;
    
    @PrePersist
    protected void onCreate() {
        if (dataHoraEntrada == null) {
            dataHoraEntrada = LocalDateTime.now();
        }
    }
}
