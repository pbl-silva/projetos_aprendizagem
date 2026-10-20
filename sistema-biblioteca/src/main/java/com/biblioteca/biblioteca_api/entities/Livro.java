package com.biblioteca.biblioteca_api.entities;

import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "livro")
public class Livro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
    @Column(nullable = false, length = 200)
    String titulo;

    @NotBlank(message = "O ISBN é obrigatório")
    @Pattern(
            regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
            message = "ISBN em formato inválido"
    )
    @Column(nullable = false, unique = true, length = 20)
    String isbn;

    @NotBlank(message = "O autor é obrigatório")
    @Column(nullable = false)
    String autor;

    @Column(name = "ano_publicacao")
    Integer anoPublicacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CategoriaLivro categoria;

    @Column(nullable = false)
    @Builder.Default
    private Boolean disponivel = true;
}
