package com.estacionamento_api.estacionamento.controller;

import com.estacionamento_api.estacionamento.dto.EntradaDTO;
import com.estacionamento_api.estacionamento.service.EntradaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/entradas")
@RequiredArgsConstructor
@Tag(name = "Entradas")
public class EntradaController {
    
    private final EntradaService service;
    
    @PostMapping
    @Operation(summary = "Registrar entrada de veículo")
    public ResponseEntity<EntradaDTO> registrarEntrada(@Valid @RequestBody EntradaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.registrarEntrada(dto));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter entrada por ID")
    public ResponseEntity<EntradaDTO> obter(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterPorId(id));
    }
    
    @GetMapping("/ativas")
    @Operation(summary = "Listar entradas ativas")
    public ResponseEntity<List<EntradaDTO>> listarAtivas() {
        return ResponseEntity.ok(service.listarAtivas());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar entrada registrada por engano (libera a vaga)")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelarEntrada(id);
        return ResponseEntity.noContent().build();
    }
}