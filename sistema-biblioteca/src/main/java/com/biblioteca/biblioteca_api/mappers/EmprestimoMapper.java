package com.biblioteca.biblioteca_api.mappers;

import com.biblioteca.biblioteca_api.dto.response.EmprestimoResponseDTO;
import com.biblioteca.biblioteca_api.dto.response.LivroResponseDTO;
import com.biblioteca.biblioteca_api.dto.response.UsuarioResponseDTO;
import com.biblioteca.biblioteca_api.entities.Emprestimo;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

public class EmprestimoMapper {

    public static EmprestimoResponseDTO toResponseDTO(Emprestimo entity, Clock clock) {
        if (entity == null) return null;

        LivroResponseDTO livroDTO = entity.getLivro() != null
                ? LivroMapper.toResponseDTO(entity.getLivro())
                : null;
        UsuarioResponseDTO usuarioDTO = entity.getUsuario() != null
                ? UsuarioMapper.toResponseDTO(entity.getUsuario())
                : null;

        Long diasRestantes = entity.getDataDevolucaoPrevista() != null
                ? Math.max(0L, entity.calcularDiasRestantes(clock))
                : null;

        return new EmprestimoResponseDTO(entity.getId(), livroDTO, usuarioDTO,
                entity.getDataEmprestimo(), entity.getDataDevolucaoPrevista(),
                entity.getDataDevolucaoReal(), entity.getStatus(), entity.getMultaCalculada(), diasRestantes);
    }

    public static List<EmprestimoResponseDTO> toResponseDTOList(List<Emprestimo> entities, Clock clock) {
        return entities.stream().map(e -> toResponseDTO(e, clock)).collect(Collectors.toList());
    }
}
