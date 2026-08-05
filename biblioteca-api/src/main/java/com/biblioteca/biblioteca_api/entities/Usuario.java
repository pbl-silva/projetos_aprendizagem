package com.biblioteca.biblioteca_api.entities;

import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false, length = 100)
    String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail em formato inválido")
    @Column(nullable = false, unique = true)
    String email;

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos")
    @Column(nullable = false, unique = true, length = 11)
    String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TipoUsuario tipoUsuario;

    @Column(name = "data_cadastro", nullable = false)
    @Builder.Default
    private LocalDate dataCadastro = LocalDate.now();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Emprestimo> emprestimosAtivos = new ArrayList<>();

    // Getter defensivo que retorna apenas empréstimos com status ATIVO
    public List<Emprestimo> getEmprestimosAtivos() {
        if (this.emprestimosAtivos == null) {
            this.emprestimosAtivos = new ArrayList<>();
        }
        return this.emprestimosAtivos.stream()
                .filter(emprestimo -> emprestimo != null && emprestimo.getStatus() == StatusEmprestimo.ATIVO)
                .collect(Collectors.toList());
    }
}