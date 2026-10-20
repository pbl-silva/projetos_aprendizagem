package com.estacionamento_api.estacionamento.model;

import jakarta.persistence.*;
import lombok.*;
import com.estacionamento_api.estacionamento.enums.TipoVaga;
import java.time.LocalDateTime;

@Entity
@Table(name = "vagas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vaga {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String numero;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVaga tipoVaga;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean disponivel = true;
    
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @OneToMany(mappedBy = "vaga", cascade = CascadeType.ALL)
    private java.util.List<Entrada> entradas;
    
    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
}
