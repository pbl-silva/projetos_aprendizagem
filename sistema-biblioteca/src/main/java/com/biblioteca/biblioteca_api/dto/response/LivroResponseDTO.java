package com.biblioteca.biblioteca_api.dto.response;

import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de um livro")
public class LivroResponseDTO {

    @Schema(description = "ID do livro", example = "1")
    private Long id;

    @Schema(description = "Título do livro", example = "Clean Code")
    private String titulo;

    @Schema(description = "ISBN do livro", example = "978-0132350884")
    private String isbn;

    @Schema(description = "Autor do livro", example = "Robert C. Martin")
    private String autor;

    @Schema(description = "Ano de publicação", example = "2008")
    private Integer anoPublicacao;

    @Schema(description = "Categoria do livro", example = "TECNICO")
    private CategoriaLivro categoria;

    @Schema(description = "Disponibilidade do livro", example = "true")
    private Boolean disponivel;
}