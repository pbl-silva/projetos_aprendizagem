package com.ecommerce.api.repository;

import com.ecommerce.api.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;


public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /**
     * Serializa alteracoes de estoque do mesmo produto para impedir que duas
     * transacoes confirmem vendas usando a mesma quantidade disponivel.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> findByIdForUpdate(@Param("id") Long id);
    
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
