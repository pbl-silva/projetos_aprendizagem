package com.biblioteca.biblioteca_api.services.impl;

import com.biblioteca.biblioteca_api.dto.request.LivroRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.LivroResponseDTO;
import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;
import com.biblioteca.biblioteca_api.exceptions.BusinessException;
import com.biblioteca.biblioteca_api.exceptions.ResourceNotFoundException;
import com.biblioteca.biblioteca_api.mappers.LivroMapper;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import com.biblioteca.biblioteca_api.services.GerenciadorLivro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LivroServiceImpl implements GerenciadorLivro {

    private final LivroRepository livroRepository;

    @Override
    public LivroResponseDTO criar(LivroRequestDTO dto) {
        if (livroRepository.findByIsbn(dto.getIsbn()).isPresent()) {
            throw new BusinessException("ISBN já cadastrado.");
        }
        Livro livro = LivroMapper.toEntity(dto);
        Livro savedLivro = livroRepository.save(livro);
        return LivroMapper.toResponseDTO(savedLivro);
    }

    @Override
    public LivroResponseDTO buscarPorId(Long id) {
        return livroRepository.findById(id)
                .map(LivroMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + id));
    }

    @Override
    public List<LivroResponseDTO> listarTodos() {
        return LivroMapper.toResponseDTOList(livroRepository.findAll());
    }

    @Override
    public List<LivroResponseDTO> listarPorCategoria(CategoriaLivro categoria) {
        return LivroMapper.toResponseDTOList(livroRepository.findByCategoria(categoria));
    }

    @Override
    public List<LivroResponseDTO> listarDisponiveis() {
        return LivroMapper.toResponseDTOList(livroRepository.findByDisponivel(true));
    }

    @Override
    public LivroResponseDTO atualizar(Long id, LivroRequestDTO dto) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado."));

        livro.setTitulo(dto.getTitulo());
        livro.setIsbn(dto.getIsbn());
        livro.setAutor(dto.getAutor());
        livro.setAnoPublicacao(dto.getAnoPublicacao());
        livro.setCategoria(dto.getCategoria());
        livroRepository.save(livro);
        return LivroMapper.toResponseDTO(livro);
    }

    @Override
    public void deletar(Long id) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado."));
        livroRepository.delete(livro);
    }
}