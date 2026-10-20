package com.ecommerce.api.controller;

import com.ecommerce.api.dto.PedidoDTO;
import com.ecommerce.api.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    /**
     * Lista todos os pedidos SEM paginação
     */
    @GetMapping("/todos")
    public ResponseEntity<List<PedidoDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    /**
     * Lista pedidos COM paginação
     */
    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "id") String ordenarPor,
            @RequestParam(defaultValue = "DESC") String direcao) {
        return ResponseEntity.ok(service.listarComPaginacao(pagina, tamanho, ordenarPor, direcao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> obter(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterPorId(id));
    }

    @PostMapping
    public ResponseEntity<PedidoDTO> criar(@Valid @RequestBody PedidoDTO dto,
                                           Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        PedidoDTO criado = admin
            ? service.criar(dto)
            : service.criarParaCliente(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/meus")
    public ResponseEntity<List<PedidoDTO>> listarMeus(Authentication authentication) {
        return ResponseEntity.ok(service.listarDoCliente(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoDTO> atualizar(@PathVariable Long id, @RequestBody PedidoDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
