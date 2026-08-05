package com.ecommerce.api.repository;

import com.ecommerce.api.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    
    /**
     * Busca produtos ativos com paginação
     */
    Page<Produto> findByAtivoTrue(Pageable pageable);
    
    /**
     * Busca produtos por nome com paginação
     */
    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    
    /**
     * Busca produtos por categoria com paginação
     */
    Page<Produto> findByCategoria_Id(Long categoriaId, Pageable pageable);
}