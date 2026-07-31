package com.estacionamento_api.estacionamento.controller;

import com.estacionamento_api.estacionamento.dto.VeiculoDTO;
import com.estacionamento_api.estacionamento.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos")
public class VeiculoController {

    private final VeiculoService service;

    @PostMapping
    @Operation(summary = "Cadastrar veículo")
    public ResponseEntity<VeiculoDTO> cadastrar(@Valid @RequestBody VeiculoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.cadastrar(dto));
    }

    @GetMapping
    @Operation(summary = "Listar todos os veículos")
    public ResponseEntity<List<VeiculoDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter veículo por ID")
    public ResponseEntity<VeiculoDTO> obter(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterPorId(id));
    }

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Obter veículo por placa")
    public ResponseEntity<VeiculoDTO> obterPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(service.obterPorPlaca(placa));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados de um veículo (a placa não pode ser alterada)")
    public ResponseEntity<VeiculoDTO> atualizar(@PathVariable Long id,
                                                @Valid @RequestBody VeiculoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir veículo (bloqueado se houver histórico de entradas)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
