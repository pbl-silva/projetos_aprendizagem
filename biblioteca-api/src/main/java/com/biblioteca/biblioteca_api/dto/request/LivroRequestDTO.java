package com.biblioteca.biblioteca_api.dto.request;

import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para criação de um livro")
public class LivroRequestDTO {

    @Schema(description = "Título do livro", example = "Clean Code", required = true)
    @NotBlank
    private String titulo;

    @Schema(description = "ISBN do livro", example = "978-0132350884", required = true)
    @NotBlank
    private String isbn;

    @Schema(description = "Autor do livro", example = "Robert C. Martin", required = true)
    @NotBlank
    private String autor;

    @Schema(description = "Ano de publicação", example = "2008")
    private Integer anoPublicacao;

    @Schema(description = "Categoria do livro", example = "TECNICO", required = true)
    @NotNull
    private CategoriaLivro categoria;
}