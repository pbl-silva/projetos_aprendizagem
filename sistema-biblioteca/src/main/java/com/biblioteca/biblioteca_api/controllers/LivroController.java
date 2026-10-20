package com.biblioteca.biblioteca_api.controllers;

import com.biblioteca.biblioteca_api.dto.request.LivroRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.LivroResponseDTO;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import com.biblioteca.biblioteca_api.exceptions.BusinessException;
import com.biblioteca.biblioteca_api.services.GerenciadorLivro;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.text.Normalizer;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/livros")
@RequiredArgsConstructor
@Tag(name = "Livros", description = "Endpoints para gerenciamento de livros")
public class LivroController {

    // DIP: Depende de abstração (interface), não de implementação concreta
    private final GerenciadorLivro gerenciadorLivro;

    @Operation(summary = "Listar todos os livros", description = "Retorna uma lista com todos os livros cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping
    public ResponseEntity<List<LivroResponseDTO>> listarTodos() {
        return ResponseEntity.ok(gerenciadorLivro.listarTodos());
    }

    @Operation(summary = "Buscar livro por ID", description = "Retorna um livro específico pelo seu identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livro encontrado"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LivroResponseDTO> buscarPorId(
            @Parameter(description = "ID do livro") @PathVariable Long id) {
        return ResponseEntity.ok(gerenciadorLivro.buscarPorId(id));
    }

    @Operation(summary = "Listar livros por categoria", description = "Retorna livros filtrados por categoria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Categoria inválida")
    })
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<LivroResponseDTO>> listarPorCategoria(
            @Parameter(description = "Categoria do livro") @PathVariable String categoria) {
        try {
            String categoriaNormalizada = normalizarString(categoria);
            CategoriaLivro cat = CategoriaLivro.valueOf(categoriaNormalizada);
            return ResponseEntity.ok(gerenciadorLivro.listarPorCategoria(cat));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Categoria inválida: " + categoria);
        }
    }

    @Operation(summary = "Listar livros disponíveis", description = "Retorna apenas os livros disponíveis para empréstimo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    @GetMapping("/disponiveis")
    public ResponseEntity<List<LivroResponseDTO>> listarDisponiveis() {
        return ResponseEntity.ok(gerenciadorLivro.listarDisponiveis());
    }

    @Operation(summary = "Criar novo livro", description = "Cadastra um novo livro no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Livro criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<LivroResponseDTO> criar(@Valid @RequestBody LivroRequestDTO dto) {
        LivroResponseDTO criado = gerenciadorLivro.criar(dto);
        URI location = URI.create(String.format("/api/livros/%d", criado.getId()));
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(summary = "Atualizar livro", description = "Atualiza um livro existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livro atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<LivroResponseDTO> atualizar(
            @Parameter(description = "ID do livro") @PathVariable Long id,
            @Valid @RequestBody LivroRequestDTO dto) {
        LivroResponseDTO atualizado = gerenciadorLivro.atualizar(id, dto);
        return ResponseEntity.ok(atualizado);
    }

    @Operation(summary = "Deletar livro", description = "Remove um livro pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Livro deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do livro") @PathVariable Long id) {
        gerenciadorLivro.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private String normalizarString(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[^\\p{ASCII}]", "");
        return normalized.toUpperCase().replace(" ", "_");
    }
}