package com.biblioteca.biblioteca_api.mappers;

import com.biblioteca.biblioteca_api.dto.request.UsuarioRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.UsuarioResponseDTO;
import com.biblioteca.biblioteca_api.entities.Usuario;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class UsuarioMapper {
    public static Usuario toEntity(UsuarioRequestDTO dto) {
        return Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .cpf(dto.getCpf())
                .tipoUsuario(dto.getTipoUsuario())
                .dataCadastro(LocalDate.now()) // ou outra lógica para a data
                .build();
    }

    public static UsuarioResponseDTO toResponseDTO(Usuario entity) {
        return new UsuarioResponseDTO(entity.getId(), entity.getNome(), entity.getEmail(),
                entity.getCpf(), entity.getTipoUsuario(), entity.getDataCadastro());
    }

    public static List<UsuarioResponseDTO> toResponseDTOList(List<Usuario> entities) {
        return entities.stream().map(UsuarioMapper::toResponseDTO).collect(Collectors.toList());
    }
}
