package com.estacionamento_api.estacionamento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Wrapper próprio para respostas paginadas. Devolver org.springframework.data.domain.Page
 * diretamente do controller funciona, mas o formato JSON dele não é
 * garantido entre versões do Spring Data — por isso expomos um formato
 * nosso, estável e explícito.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginaDTO<T> {
    private List<T> conteudo;
    private int paginaAtual;
    private int tamanhoPagina;
    private long totalElementos;
    private int totalPaginas;
    private boolean ultimaPagina;
}
