package com.biblioteca.biblioteca_api.dto.request;

import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para criação de um usuário")
public class UsuarioRequestDTO {

    @Schema(description = "Nome do usuário", example = "João da Silva", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;

    @Schema(description = "E-mail do usuário", example = "joao.silva@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Email
    private String email;

    @Schema(description = "CPF do usuário", example = "12345678901", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Pattern(regexp = "\\d{11}")
    private String cpf;

    @Schema(description = "Tipo de usuário", example = "COMUM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private TipoUsuario tipoUsuario;
}
