package com.biblioteca.biblioteca_api.services;

import com.biblioteca.biblioteca_api.dto.request.LivroRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.LivroResponseDTO;
import com.biblioteca.biblioteca_api.enums.CategoriaLivro;

import java.util.List;

public interface GerenciadorLivro {
    LivroResponseDTO criar(LivroRequestDTO dto);
    LivroResponseDTO atualizar(Long id, LivroRequestDTO dto);
    void deletar(Long id);
    LivroResponseDTO buscarPorId(Long id);
    List<LivroResponseDTO> listarTodos();
    List<LivroResponseDTO> listarPorCategoria(CategoriaLivro categoria);
    List<LivroResponseDTO> listarDisponiveis();
}