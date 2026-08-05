package com.biblioteca.biblioteca_api.entities;

import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "emprestimo")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull(message = "A data do empréstimo é obrigatória")
    @Column(name = "data_emprestimo", nullable = false, updatable = false)
    @Builder.Default
    private LocalDate dataEmprestimo = LocalDate.now();

    @NotNull(message = "A data de devolução prevista é obrigatória")
    @Column(name = "data_devolucao_prevista", nullable = false)
    private LocalDate dataDevolucaoPrevista;

    @Column(name = "data_devolucao_real")
    private LocalDate dataDevolucaoReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusEmprestimo status = StatusEmprestimo.ATIVO;

    @Column(name = "multa_calculada", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal multaCalculada = BigDecimal.ZERO;

    @NotNull(message = "O livro é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @NotNull(message = "O usuário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public boolean isAtrasado(LocalDate referencia) {
        return status == StatusEmprestimo.ATIVO
                && referencia.isAfter(dataDevolucaoPrevista);
    }

    public boolean isAtrasado(Clock clock) {
        return isAtrasado(LocalDate.now(clock));
    }

    public boolean isAtrasado() {
        return isAtrasado(LocalDate.now());
    }

    public long calcularDiasRestantes(LocalDate referencia) {
        return ChronoUnit.DAYS.between(referencia, dataDevolucaoPrevista);
    }

    public long calcularDiasRestantes(Clock clock) {
        return calcularDiasRestantes(LocalDate.now(clock));
    }

    public long calcularDiasRestantes() {
        return calcularDiasRestantes(LocalDate.now());
    }
}