package com.ecommerce.api.controller;

import com.ecommerce.api.dto.ProdutoDTO;
import com.ecommerce.api.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Produtos", description = "Endpoints para gerenciar produtos")
public class ProdutoController {
    
    private final ProdutoService produtoService;
    
    /**
     * Lista todos os produtos SEM paginação
     */
    @GetMapping("/todos")
    @Operation(summary = "Lista todos os produtos sem paginação")
    public ResponseEntity<List<ProdutoDTO>> listarTodos() {
        log.info("GET /api/produtos/todos");
        List<ProdutoDTO> produtos = produtoService.listarTodos();
        return ResponseEntity.ok(produtos);
    }
    
    /**
     * Lista produtos COM paginação
     */
    @GetMapping
    @Operation(summary = "Lista produtos com paginação")
    public ResponseEntity<Page<ProdutoDTO>> listar(
            @Parameter(description = "Número da página (começa em 0)")
            @RequestParam(defaultValue = "0") int pagina,
            
            @Parameter(description = "Quantidade de registros por página")
            @RequestParam(defaultValue = "10") int tamanho,
            
            @Parameter(description = "Campo para ordenação")
            @RequestParam(defaultValue = "id") String ordenarPor,
            
            @Parameter(description = "Direção da ordenação: ASC ou DESC")
            @RequestParam(defaultValue = "DESC") String direcao) {
        
        log.info("GET /api/produtos - Página: {}, Tamanho: {}, Ordenar por: {}, Direção: {}", 
            pagina, tamanho, ordenarPor, direcao);
        
        Page<ProdutoDTO> produtos = produtoService.listarComPaginacao(pagina, tamanho, ordenarPor, direcao);
        return ResponseEntity.ok(produtos);
    }
    
    /**
     * Lista produtos ativos COM paginação
     */
    @GetMapping("/ativos")
    @Operation(summary = "Lista apenas produtos ativos com paginação")
    public ResponseEntity<Page<ProdutoDTO>> listarAtivos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        
        log.info("GET /api/produtos/ativos - Página: {}, Tamanho: {}", pagina, tamanho);
        
        Page<ProdutoDTO> produtos = produtoService.listarAtivosComPaginacao(pagina, tamanho);
        return ResponseEntity.ok(produtos);
    }
    
    /**
     * Busca produtos por nome COM paginação
     */
    @GetMapping("/buscar")
    @Operation(summary = "Busca produtos por nome com paginação")
    public ResponseEntity<Page<ProdutoDTO>> buscarPorNome(
            @Parameter(description = "Nome do produto (busca parcial)")
            @RequestParam String nome,
            
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        
        log.info("GET /api/produtos/buscar?nome={} - Página: {}, Tamanho: {}", nome, pagina, tamanho);
        
        Page<ProdutoDTO> produtos = produtoService.buscarPorNome(nome, pagina, tamanho);
        return ResponseEntity.ok(produtos);
    }
    
    /**
     * Busca produtos por categoria COM paginação
     */
    @GetMapping("/categoria/{categoriaId}")
    @Operation(summary = "Lista produtos de uma categoria com paginação")
    public ResponseEntity<Page<ProdutoDTO>> buscarPorCategoria(
            @Parameter(description = "ID da categoria")
            @PathVariable Long categoriaId,
            
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        
        log.info("GET /api/produtos/categoria/{} - Página: {}, Tamanho: {}", categoriaId, pagina, tamanho);
        
        Page<ProdutoDTO> produtos = produtoService.buscarPorCategoria(categoriaId, pagina, tamanho);
        return ResponseEntity.ok(produtos);
    }
    
    /**
     * Obtém produto por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtém um produto por ID")
    public ResponseEntity<ProdutoDTO> obterPorId(
            @Parameter(description = "ID do produto")
            @PathVariable Long id) {
        
        log.info("GET /api/produtos/{}", id);
        
        ProdutoDTO produto = produtoService.obterPorId(id);
        return ResponseEntity.ok(produto);
    }
    
    /**
     * Cria novo produto
     */
    @PostMapping
    @Operation(summary = "Cria novo produto")
    public ResponseEntity<ProdutoDTO> criar(@Valid @RequestBody ProdutoDTO dto) {
        log.info("POST /api/produtos - Criando produto: {}", dto.getNome());
        
        ProdutoDTO produto = produtoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(produto);
    }
    
    /**
     * Atualiza produto
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um produto")
    public ResponseEntity<ProdutoDTO> atualizar(
            @Parameter(description = "ID do produto")
            @PathVariable Long id,
            
            @Valid @RequestBody ProdutoDTO dto) {
        
        log.info("PUT /api/produtos/{} - Atualizando produto", id);
        
        ProdutoDTO produto = produtoService.atualizar(id, dto);
        return ResponseEntity.ok(produto);
    }
    
    /**
     * Deleta produto
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta um produto")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID do produto")
            @PathVariable Long id) {
        
        log.info("DELETE /api/produtos/{}", id);
        
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Diminui estoque de um produto
     */
    @PatchMapping("/{id}/estoque/diminuir")
    @Operation(summary = "Diminui o estoque de um produto")
    public ResponseEntity<Integer> diminuirEstoque(
            @Parameter(description = "ID do produto")
            @PathVariable Long id,
            
            @Parameter(description = "Quantidade a diminuir")
            @RequestParam Integer quantidade) {
        
        log.info("PATCH /api/produtos/{}/estoque/diminuir?quantidade={}", id, quantidade);
        
        Integer novoEstoque = produtoService.diminuirEstoque(id, quantidade);
        return ResponseEntity.ok(novoEstoque);
    }
    
    /**
     * Aumenta estoque de um produto
     */
    @PatchMapping("/{id}/estoque/aumentar")
    @Operation(summary = "Aumenta o estoque de um produto")
    public ResponseEntity<Integer> aumentarEstoque(
            @Parameter(description = "ID do produto")
            @PathVariable Long id,
            
            @Parameter(description = "Quantidade a aumentar")
            @RequestParam Integer quantidade) {
        
        log.info("PATCH /api/produtos/{}/estoque/aumentar?quantidade={}", id, quantidade);
        
        Integer novoEstoque = produtoService.aumentarEstoque(id, quantidade);
        return ResponseEntity.ok(novoEstoque);
    }
}