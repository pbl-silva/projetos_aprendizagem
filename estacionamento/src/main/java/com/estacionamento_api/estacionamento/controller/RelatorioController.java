package com.estacionamento_api.estacionamento.controller;

import com.estacionamento_api.estacionamento.dto.PaginaDTO;
import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios")
public class RelatorioController {
    
    private final RelatorioService service;
    
    @GetMapping("/ocupacao")
    @Operation(summary = "Taxa de ocupação atual")
    public ResponseEntity<Map<String, Object>> ocupacao() {
        return ResponseEntity.ok(service.obterTaxaOcupacao());
    }
    
    @GetMapping("/faturamento")
    @Operation(summary = "Faturamento do período")
    public ResponseEntity<Map<String, Object>> faturamento(
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fim) {
        return ResponseEntity.ok(service.obterFaturamento(inicio, fim));
    }

    @GetMapping("/saidas")
    @Operation(summary = "Histórico de saídas do período (paginado). " +
        "Parâmetros de paginação: page (0-based), size, sort (ex: sort=dataHoraSaida,desc)")
    public ResponseEntity<PaginaDTO<ReciboDTO>> saidasDoPeriodo(
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fim,
            @PageableDefault(size = 10, sort = "dataHoraSaida", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(service.listarSaidasPaginado(inicio, fim, pageable));
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Exportar relatório do período em PDF " +
        "(ocupação + faturamento + histórico completo de saídas)")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fim) throws IOException {
        byte[] pdf = service.gerarRelatorioPdf(inicio, fim);

        String nomeArquivo = "relatorio-estacionamento-"
            + inicio.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-a-"
            + fim.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + ".pdf";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nomeArquivo)
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
