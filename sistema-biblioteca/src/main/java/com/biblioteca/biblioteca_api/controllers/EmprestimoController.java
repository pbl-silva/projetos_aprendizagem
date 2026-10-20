package com.biblioteca.biblioteca_api.controllers;

import com.biblioteca.biblioteca_api.dto.request.EmprestimoRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.EmprestimoResponseDTO;
import com.biblioteca.biblioteca_api.services.GerenciadorEmprestimo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/emprestimos")
@RequiredArgsConstructor
@Tag(name = "Empréstimos", description = "Endpoints para gerenciamento de empréstimos")
public class EmprestimoController {

    // DIP: Depende de abstração (interface), não de implementação concreta
    private final GerenciadorEmprestimo gerenciadorEmprestimo;

    @Operation(summary = "Listar todos os empréstimos", description = "Retorna uma lista com todos os empréstimos cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping
    public ResponseEntity<List<EmprestimoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(gerenciadorEmprestimo.listarTodos());
    }

    @Operation(summary = "Buscar empréstimo por ID", description = "Retorna um empréstimo específico pelo seu identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empréstimo encontrado"),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmprestimoResponseDTO> buscarPorId(
            @Parameter(description = "ID do empréstimo") @PathVariable Long id) {
        return ResponseEntity.ok(gerenciadorEmprestimo.buscarPorId(id));
    }

    @Operation(summary = "Listar empréstimos por usuário", description = "Retorna todos os empréstimos de um usuário específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<EmprestimoResponseDTO>> listarPorUsuario(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(gerenciadorEmprestimo.listarPorUsuario(usuarioId));
    }

    @Operation(summary = "Listar empréstimos ativos", description = "Retorna todos os empréstimos que ainda não foram devolvidos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    @GetMapping("/ativos")
    public ResponseEntity<List<EmprestimoResponseDTO>> listarAtivos() {
        return ResponseEntity.ok(gerenciadorEmprestimo.listarAtivos());
    }

    @Operation(summary = "Listar empréstimos atrasados", description = "Retorna todos os empréstimos com devolução em atraso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    @GetMapping("/atrasados")
    public ResponseEntity<List<EmprestimoResponseDTO>> listarAtrasados() {
        return ResponseEntity.ok(gerenciadorEmprestimo.listarAtrasados());
    }

    @Operation(summary = "Realizar empréstimo", description = "Registra um novo empréstimo de livro")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empréstimo realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou livro indisponível"),
            @ApiResponse(responseCode = "404", description = "Livro ou usuário não encontrado")
    })
    @PostMapping
    public ResponseEntity<EmprestimoResponseDTO> realizarEmprestimo(@Valid @RequestBody EmprestimoRequestDTO dto) {
        EmprestimoResponseDTO criado = gerenciadorEmprestimo.realizarEmprestimo(dto);
        URI location = URI.create(String.format("/api/emprestimos/%d", criado.getId()));
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(summary = "Devolver livro", description = "Registra a devolução de um livro emprestado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devolução registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Empréstimo já foi devolvido"),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    })
    // aceita PUT e PATCH para compatibilidade com clientes que usam qualquer um dos métodos
    @RequestMapping(value = "/{id}/devolver", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<EmprestimoResponseDTO> devolverLivro(
            @Parameter(description = "ID do empréstimo") @PathVariable Long id) {
        EmprestimoResponseDTO devolvido = gerenciadorEmprestimo.devolverLivro(id);
        return ResponseEntity.ok(devolvido);
    }
}