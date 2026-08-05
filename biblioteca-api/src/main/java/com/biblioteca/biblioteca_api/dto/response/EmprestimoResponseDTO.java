package com.biblioteca.biblioteca_api.dto.response;

import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de um empréstimo")
public class EmprestimoResponseDTO {

    @Schema(description = "ID do empréstimo", example = "1")
    private Long id;

    @Schema(description = "Dados do livro associado", required = true)
    private LivroResponseDTO livro;

    @Schema(description = "Dados do usuário associado", required = true)
    private UsuarioResponseDTO usuario;

    @Schema(description = "Data do empréstimo", example = "2026-06-01")
    private LocalDate dataEmprestimo;

    @Schema(description = "Data de devolução prevista", example = "2026-06-15")
    private LocalDate dataDevolucaoPrevista;

    @Schema(description = "Data de devolução real", example = "2026-06-14")
    private LocalDate dataDevolucaoReal;

    @Schema(description = "Status do empréstimo", example = "ATIVO")
    private StatusEmprestimo status;

    @Schema(description = "Multa calculada, se houver", example = "0.00")
    private BigDecimal multaCalculada;

    @Schema(description = "Dias restantes para devolução", example = "5")
    private Long diasRestantes;
}