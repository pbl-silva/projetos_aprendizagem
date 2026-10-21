package com.biblioteca.biblioteca_api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ImportacaoAcervo {
    @Id
    private String versao;

    protected ImportacaoAcervo() {}

    public ImportacaoAcervo(String versao) {
        this.versao = versao;
    }
}
