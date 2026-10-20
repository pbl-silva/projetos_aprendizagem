package com.biblioteca.biblioteca_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para criação de um empréstimo")
public class EmprestimoRequestDTO {

    @Schema(description = "ID do livro", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long livroId;

    @Schema(description = "ID do usuário", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long usuarioId;
}
