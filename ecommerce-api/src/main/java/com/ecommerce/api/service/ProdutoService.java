package com.ecommerce.api.service;

import com.ecommerce.api.dto.ProdutoDTO;
import com.ecommerce.api.exception.EstoqueInsuficienteException;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Categoria;
import com.ecommerce.api.model.Produto;
import com.ecommerce.api.repository.CategoriaRepository;
import com.ecommerce.api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProdutoService {
    
    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    
    /**
     * Cria novo produto
     */
    public ProdutoDTO criar(ProdutoDTO dto) {
        log.info("Criando novo produto: {}", dto.getNome());
        
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria não encontrada com ID: " + dto.getCategoriaId()
            ));
        
        Produto produto = Produto.builder()
            .nome(dto.getNome())
            .descricao(dto.getDescricao())
            .preco(dto.getPreco())
            .estoque(dto.getEstoque())
            .categoria(categoria)
            .ativo(true)
            .build();
        
        Produto salvo = produtoRepository.save(produto);
        log.debug("Produto criado com ID: {}", salvo.getId());
        
        return converterParaDTO(salvo);
    }
    
    /**
     * Obtém produto por ID
     */
    @Transactional(readOnly = true)
    public ProdutoDTO obterPorId(Long id) {
        log.debug("Buscando produto com ID: {}", id);
        
        Produto produto = produtoRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Produto não encontrado com ID: {}", id);
                return new RecursoNaoEncontradoException(
                    "Produto não encontrado com ID: " + id
                );
            });
        
        return converterParaDTO(produto);
    }
    
    /**
     * Lista todos os produtos SEM paginação
     */
    @Transactional(readOnly = true)
    public List<ProdutoDTO> listarTodos() {
        log.info("Listando todos os produtos");
        
        return produtoRepository.findAll()
            .stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Lista produtos COM paginação
     * @param pagina Número da página (começa em 0)
     * @param tamanho Quantidade de registros por página
     * @param ordenarPor Campo para ordenação (padrão: id)
     * @param direcao Direção da ordenação: ASC ou DESC
     * @return Page com produtos
     */
    @Transactional(readOnly = true)
    public Page<ProdutoDTO> listarComPaginacao(
            int pagina, 
            int tamanho, 
            String ordenarPor, 
            String direcao) {
        
        log.info("Listando produtos com paginação - Página: {}, Tamanho: {}, Ordenar por: {}, Direção: {}", 
            pagina, tamanho, ordenarPor, direcao);
        
        // Validar tamanho máximo
        if (tamanho > 100) {
            tamanho = 100;
            log.warn("Tamanho da página reduzido para 100");
        }
        
        // Definir direção da ordenação
        Sort.Direction direction = Sort.Direction.fromString(direcao.toUpperCase());
        Sort sort = Sort.by(direction, ordenarPor);
        
        // Criar Pageable
        Pageable pageable = PageRequest.of(pagina, tamanho, sort);
        
        // Buscar página
        Page<Produto> produtos = produtoRepository.findAll(pageable);
        
        log.debug("Total de produtos encontrados: {}, Página atual: {}, Total de páginas: {}", 
            produtos.getTotalElements(), produtos.getNumber(), produtos.getTotalPages());
        
        return produtos.map(this::converterParaDTO);
    }
    
    /**
     * Lista produtos ativos COM paginação
     */
    @Transactional(readOnly = true)
    public Page<ProdutoDTO> listarAtivosComPaginacao(int pagina, int tamanho) {
        log.info("Listando produtos ativos com paginação");
        
        if (tamanho > 100) tamanho = 100;
        
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());
        Page<Produto> produtos = produtoRepository.findByAtivoTrue(pageable);
        
        return produtos.map(this::converterParaDTO);
    }
    
    /**
     * Busca produtos por nome COM paginação
     */
    @Transactional(readOnly = true)
    public Page<ProdutoDTO> buscarPorNome(String nome, int pagina, int tamanho) {
        log.info("Buscando produtos por nome: {}", nome);
        
        if (tamanho > 100) tamanho = 100;
        
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());
        Page<Produto> produtos = produtoRepository.findByNomeContainingIgnoreCase(nome, pageable);
        
        log.debug("Encontrados {} produtos com nome contendo: {}", produtos.getTotalElements(), nome);
        
        return produtos.map(this::converterParaDTO);
    }
    
    /**
     * Busca produtos por categoria COM paginação
     */
    @Transactional(readOnly = true)
    public Page<ProdutoDTO> buscarPorCategoria(Long categoriaId, int pagina, int tamanho) {
        log.info("Buscando produtos da categoria: {}", categoriaId);
        
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new RecursoNaoEncontradoException(
                "Categoria não encontrada com ID: " + categoriaId
            );
        }
        
        if (tamanho > 100) tamanho = 100;
        
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());
        Page<Produto> produtos = produtoRepository.findByCategoria_Id(categoriaId, pageable);
        
        return produtos.map(this::converterParaDTO);
    }
    
    /**
     * Atualiza produto
     */
    public ProdutoDTO atualizar(Long id, ProdutoDTO dto) {
        log.info("Atualizando produto com ID: {}", id);
        
        Produto produto = produtoRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto não encontrado com ID: " + id
            ));
        
        if (dto.getCategoriaId() != null && !dto.getCategoriaId().equals(produto.getCategoria().getId())) {
            Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Categoria não encontrada com ID: " + dto.getCategoriaId()
                ));
            produto.setCategoria(categoria);
        }
        
        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setPreco(dto.getPreco());
        produto.setEstoque(dto.getEstoque());
        
        Produto atualizado = produtoRepository.save(produto);
        log.debug("Produto atualizado com sucesso: {}", id);
        
        return converterParaDTO(atualizado);
    }
    
    /**
     * Deleta produto
     */
    public void deletar(Long id) {
        log.info("Deletando produto com ID: {}", id);
        
        if (!produtoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                "Produto não encontrado com ID: " + id
            );
        }
        
        produtoRepository.deleteById(id);
        log.debug("Produto deletado com sucesso: {}", id);
    }
    
    /**
     * Diminui o estoque de um produto
     */
    public Integer diminuirEstoque(Long produtoId, Integer quantidade) {
        log.info("Diminuindo estoque do produto {} em {} unidades", produtoId, quantidade);

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        Produto produto = produtoRepository.findByIdForUpdate(produtoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto não encontrado com ID: " + produtoId
            ));
        
        if (produto.getEstoque() < quantidade) {
            log.warn("Estoque insuficiente para produto {}: disponível {}, solicitado {}", 
                produtoId, produto.getEstoque(), quantidade);
            throw new EstoqueInsuficienteException(
                "Estoque insuficiente. Disponível: " + produto.getEstoque() + 
                ", Solicitado: " + quantidade
            );
        }
        
        produto.setEstoque(produto.getEstoque() - quantidade);
        Produto atualizado = produtoRepository.save(produto);
        
        log.debug("Estoque diminuído com sucesso. Novo estoque: {}", atualizado.getEstoque());
        
        return atualizado.getEstoque();
    }
    
    /**
     * Aumenta o estoque de um produto
     */
    public Integer aumentarEstoque(Long produtoId, Integer quantidade) {
        log.info("Aumentando estoque do produto {} em {} unidades", produtoId, quantidade);

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        Produto produto = produtoRepository.findByIdForUpdate(produtoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto não encontrado com ID: " + produtoId
            ));
        
        try {
            produto.setEstoque(Math.addExact(produto.getEstoque(), quantidade));
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Estoque excede o limite permitido", ex);
        }
        Produto atualizado = produtoRepository.save(produto);
        
        log.debug("Estoque aumentado com sucesso. Novo estoque: {}", atualizado.getEstoque());
        
        return atualizado.getEstoque();
    }
    
    /**
     * Converte Produto para ProdutoDTO
     */
    private ProdutoDTO converterParaDTO(Produto produto) {
        return ProdutoDTO.builder()
            .id(produto.getId())
            .nome(produto.getNome())
            .descricao(produto.getDescricao())
            .preco(produto.getPreco())
            .estoque(produto.getEstoque())
            .categoriaId(produto.getCategoria().getId())
            .ativo(produto.getAtivo())
            .build();
    }
}
