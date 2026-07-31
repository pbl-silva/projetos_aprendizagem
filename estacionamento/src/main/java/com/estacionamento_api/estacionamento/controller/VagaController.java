package com.estacionamento_api.estacionamento.controller;

import com.estacionamento_api.estacionamento.dto.VagaDTO;
import com.estacionamento_api.estacionamento.service.VagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/vagas")
@RequiredArgsConstructor
@Tag(name = "Vagas")
public class VagaController {
    
    private final VagaService service;
    
    @GetMapping
    @Operation(summary = "Listar todas as vagas")
    public ResponseEntity<List<VagaDTO>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }
    
    @GetMapping("/disponiveis")
    @Operation(summary = "Listar vagas disponíveis")
    public ResponseEntity<List<VagaDTO>> listarDisponiveis() {
        return ResponseEntity.ok(service.listarDisponiveis());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter vaga por ID")
    public ResponseEntity<VagaDTO> obter(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uma vaga (uso administrativo: manutenção ou reclassificação de tipo)")
    public ResponseEntity<VagaDTO> atualizar(@PathVariable Long id, @RequestBody VagaDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }
}
  