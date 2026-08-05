package com.biblioteca.biblioteca_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estatísticas de um usuário")
public class EstatisticasDTO {

    @Schema(description = "Total de empréstimos realizados", example = "15")
    private Long totalEmprestimos;

    @Schema(description = "Empréstimos ativos no momento", example = "2")
    private Long emprestimosAtivos;

    @Schema(description = "Total de multas acumuladas", example = "50.00")
    private BigDecimal multasAcumuladas;

    @Schema(description = "Livros devolvidos no prazo", example = "12")
    private Long livrosNoPrazo;

    @Schema(description = "Livros devolvidos com atraso", example = "3")
    private Long livrosAtrasados;
}