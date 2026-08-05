package com.biblioteca.biblioteca_api.dto.response;

import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de um usuário")
public class UsuarioResponseDTO {

    @Schema(description = "ID do usuário", example = "1")
    private Long id;

    @Schema(description = "Nome do usuário", example = "João da Silva")
    private String nome;

    @Schema(description = "E-mail do usuário", example = "joao.silva@email.com")
    private String email;

    @Schema(description = "CPF do usuário", example = "12345678901")
    private String cpf;

    @Schema(description = "Tipo de usuário", example = "COMUM")
    private TipoUsuario tipoUsuario;

    @Schema(description = "Data de cadastro do usuário", example = "2026-06-01")
    private LocalDate dataCadastro;
}