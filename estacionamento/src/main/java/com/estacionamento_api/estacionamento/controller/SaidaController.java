package com.estacionamento_api.estacionamento.controller;

import com.estacionamento_api.estacionamento.dto.ReciboDTO;
import com.estacionamento_api.estacionamento.dto.SaidaDTO;
import com.estacionamento_api.estacionamento.service.SaidaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/saidas")
@RequiredArgsConstructor
@Tag(name = "Saídas")
public class SaidaController {
    
    private final SaidaService service;
    
    @PostMapping
    @Operation(summary = "Registrar saída e gerar recibo")
    public ResponseEntity<ReciboDTO> registrarSaida(@Valid @RequestBody SaidaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.registrarSaida(dto));
    }
    
    @GetMapping("/recibo/{id}")
    @Operation(summary = "Obter recibo por ID")
    public ResponseEntity<ReciboDTO> obterRecibo(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterRecibo(id));
    }
}