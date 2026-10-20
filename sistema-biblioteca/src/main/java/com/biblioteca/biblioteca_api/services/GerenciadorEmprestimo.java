package com.biblioteca.biblioteca_api.services;

import com.biblioteca.biblioteca_api.dto.request.EmprestimoRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.EmprestimoResponseDTO;

import java.util.List;

public interface GerenciadorEmprestimo {
    EmprestimoResponseDTO realizarEmprestimo(EmprestimoRequestDTO dto);
    EmprestimoResponseDTO devolverLivro(Long emprestimoId);
    EmprestimoResponseDTO buscarPorId(Long id);
    List<EmprestimoResponseDTO> listarTodos();
    List<EmprestimoResponseDTO> listarPorUsuario(Long usuarioId);
    List<EmprestimoResponseDTO> listarAtivos();
    List<EmprestimoResponseDTO> listarAtrasados();
}