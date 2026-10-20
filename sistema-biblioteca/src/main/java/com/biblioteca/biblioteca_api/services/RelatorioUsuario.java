package com.biblioteca.biblioteca_api.services;

import com.biblioteca.biblioteca_api.dto.response.EstatisticasDTO;

public interface RelatorioUsuario {
    byte[] gerarRelatorioEmprestimos(Long usuarioId);
    EstatisticasDTO calcularEstatisticas(Long usuarioId);
}