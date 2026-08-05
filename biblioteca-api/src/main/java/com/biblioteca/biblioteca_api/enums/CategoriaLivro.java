package com.biblioteca.biblioteca_api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CategoriaLivro {

    FICCAO("Ficção"),
    NAO_FICCAO("Não Ficção"),
    TECNICO("Técnico"),
    ROMANCE("Romance"),
    SUSPENSE("Suspense"),
    BIOGRAFIA("Biografia");

    private final String descricao;

}
