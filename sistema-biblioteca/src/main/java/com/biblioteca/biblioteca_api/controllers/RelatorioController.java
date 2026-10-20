package com.biblioteca.biblioteca_api.controllers;

import com.biblioteca.biblioteca_api.dto.response.EstatisticasDTO;
import com.biblioteca.biblioteca_api.services.RelatorioUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Endpoints para geração de relatórios e estatísticas")
public class RelatorioController {

    // DIP: Depende de abstração (interface), não de implementação concreta
    private final RelatorioUsuario relatorioUsuario;

    @Operation(summary = "Gerar relatório de empréstimos do usuário",
            description = "Gera um relatório em PDF com o histórico de empréstimos do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/usuario/{usuarioId}/emprestimos")
    public ResponseEntity<byte[]> gerarRelatorioEmprestimos(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId) {
        byte[] pdf = relatorioUsuario.gerarRelatorioEmprestimos(usuarioId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "relatorio-emprestimos-" + usuarioId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    @Operation(summary = "Obter estatísticas do usuário",
            description = "Retorna estatísticas detalhadas sobre os empréstimos do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/usuario/{usuarioId}/estatisticas")
    public ResponseEntity<EstatisticasDTO> calcularEstatisticas(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId) {
        EstatisticasDTO estatisticas = relatorioUsuario.calcularEstatisticas(usuarioId);
        return ResponseEntity.ok(estatisticas);
    }
}