package com.ecommerce;

import com.ecommerce.api.model.Categoria;
import com.ecommerce.api.model.Produto;
import com.ecommerce.api.repository.CategoriaRepository;
import com.ecommerce.api.repository.ProdutoRepository;
import com.ecommerce.api.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProdutoServiceTest {
    
    @Autowired
    private ProdutoService produtoService;
    
    @Autowired
    private ProdutoRepository produtoRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    private Categoria categoria;
    private Produto produto;
    
    @BeforeEach
    public void setUp() {
        // Criar categoria
        categoria = Categoria.builder()
            .nome("Eletrônicos")
            .descricao("Produtos eletrônicos")
            .ativo(true)
            .build();
        categoria = categoriaRepository.save(categoria);
        
        // Criar produto com BigDecimal
        produto = Produto.builder()
            .nome("Notebook")
            .descricao("Notebook Dell")
            .preco(new BigDecimal("2500.00"))
            .estoque(10)
            .categoria(categoria)
            .ativo(true)
            .build();
        produto = produtoRepository.save(produto);
    }
    
    @Test
    public void testDiminuirEstoque() {
        Integer novoEstoque = produtoService.diminuirEstoque(produto.getId(), 3);
        assertEquals(7, novoEstoque);
    }
    
    @Test
    public void testDiminuirEstoqueInsuficiente() {
        assertThrows(IllegalArgumentException.class, () -> {
            produtoService.diminuirEstoque(produto.getId(), 15);
        });
    }
    
    @Test
    public void testAumentarEstoque() {
        Integer novoEstoque = produtoService.aumentarEstoque(produto.getId(), 5);
        assertEquals(15, novoEstoque);
    }
}