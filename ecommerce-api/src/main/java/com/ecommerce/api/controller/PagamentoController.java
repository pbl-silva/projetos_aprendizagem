package com.ecommerce.api.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.api.dto.PagamentoDTO;
import com.ecommerce.api.enums.StatusPagamento;
import com.ecommerce.api.service.PagamentoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService service;

    @PostMapping
    public ResponseEntity<PagamentoDTO> criar(@Valid @RequestBody PagamentoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.criar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagamentoDTO> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterPorId(id));
    }

    /**
     * Lista todos os pagamentos SEM paginação
     */
    @GetMapping("/todos")
    public ResponseEntity<List<PagamentoDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    /**
     * Lista pagamentos COM paginação
     */
    @GetMapping
    public ResponseEntity<Page<PagamentoDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "id") String ordenarPor,
            @RequestParam(defaultValue = "DESC") String direcao) {
        return ResponseEntity.ok(service.listarComPaginacao(pagina, tamanho, ordenarPor, direcao));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagamentoDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PagamentoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PagamentoDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestParam StatusPagamento status) {
        return ResponseEntity.ok(service.atualizarStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}