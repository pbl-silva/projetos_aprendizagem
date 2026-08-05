package com.biblioteca.biblioteca_api.services;

import com.biblioteca.biblioteca_api.dto.request.UsuarioRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.UsuarioResponseDTO;

import java.util.List;

public interface GerenciadorUsuario {
    UsuarioResponseDTO criar(UsuarioRequestDTO dto);
    UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto);
    void deletar(Long id);
    UsuarioResponseDTO buscarPorId(Long id);
    UsuarioResponseDTO buscarPorEmail(String email);
    List<UsuarioResponseDTO> listarTodos();
}