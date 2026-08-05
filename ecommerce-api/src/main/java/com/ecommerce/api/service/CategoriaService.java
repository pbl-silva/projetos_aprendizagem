package com.ecommerce.api.service;

import com.ecommerce.api.dto.CategoriaDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Categoria;
import com.ecommerce.api.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
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
public class CategoriaService {
    
    private final CategoriaRepository categoriaRepository;
    
    public CategoriaDTO criar(CategoriaDTO dto) {
        Categoria categoria = Categoria.builder()
            .nome(dto.getNome())
            .descricao(dto.getDescricao())
            .ativo(true)
            .build();
        
        Categoria salva = categoriaRepository.save(categoria);
        return converterParaDTO(salva);
    }
    
    @Transactional(readOnly = true)
    public CategoriaDTO obterPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria não encontrada com ID: " + id
            ));
        return converterParaDTO(categoria);
    }
    
    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarTodas() {
        return categoriaRepository.findAll()
            .stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CategoriaDTO> listarComPaginacao(int pagina, int tamanho, String ordenarPor, String direcao) {
        if (tamanho > 100) {
            tamanho = 100;
        }
        Sort.Direction direction = Sort.Direction.fromString(direcao.toUpperCase());
        Sort sort = Sort.by(direction, ordenarPor);
        Pageable pageable = PageRequest.of(pagina, tamanho, sort);
        return categoriaRepository.findAll(pageable).map(this::converterParaDTO);
    }
    
    public CategoriaDTO atualizar(Long id, CategoriaDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria não encontrada com ID: " + id
            ));
        
        categoria.setNome(dto.getNome());
        categoria.setDescricao(dto.getDescricao());
        
        Categoria atualizada = categoriaRepository.save(categoria);
        return converterParaDTO(atualizada);
    }
    
    public void deletar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                "Categoria não encontrada com ID: " + id
            );
        }
        categoriaRepository.deleteById(id);
    }
    
    private CategoriaDTO converterParaDTO(Categoria categoria) {
        return CategoriaDTO.builder()
            .id(categoria.getId())
            .nome(categoria.getNome())
            .descricao(categoria.getDescricao())
            .ativo(categoria.getAtivo())
            .build();
    }
}