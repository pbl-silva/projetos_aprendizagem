package com.biblioteca.biblioteca_api.mappers;

import com.biblioteca.biblioteca_api.dto.request.LivroRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.LivroResponseDTO;
import com.biblioteca.biblioteca_api.entities.Livro;

import java.util.List;
import java.util.stream.Collectors;

public class LivroMapper {
    public static Livro toEntity(LivroRequestDTO dto) {
        return Livro.builder()
                .titulo(dto.getTitulo())
                .isbn(dto.getIsbn())
                .autor(dto.getAutor())
                .anoPublicacao(dto.getAnoPublicacao())
                .categoria(dto.getCategoria())
                .build();
    }

    public static LivroResponseDTO toResponseDTO(Livro entity) {
        return new LivroResponseDTO(entity.getId(), entity.getTitulo(), entity.getIsbn(),
                entity.getAutor(), entity.getAnoPublicacao(), entity.getCategoria(), entity.getDisponivel());
    }

    public static List<LivroResponseDTO> toResponseDTOList(List<Livro> entities) {
        return entities.stream().map(LivroMapper::toResponseDTO).collect(Collectors.toList());
    }
}