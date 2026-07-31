package com.estacionamento_api.estacionamento.model;

import jakarta.persistence.*;
import lombok.*;
import com.estacionamento_api.estacionamento.enums.TipoVeiculo;
import java.time.LocalDateTime;

@Entity
@Table(name = "veiculos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veiculo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 10)
    private String placa;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVeiculo tipoVeiculo;
    
    @Column(nullable = false, length = 50)
    private String marca;
    
    @Column(nullable = false, length = 50)
    private String modelo;
    
    @Column(length = 30)
    private String cor;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean pcd = false;
    
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL)
    private java.util.List<Entrada> entradas;
    
    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
}