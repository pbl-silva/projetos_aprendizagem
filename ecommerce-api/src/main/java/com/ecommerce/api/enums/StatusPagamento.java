package com.ecommerce.api.enums;

public enum StatusPagamento {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    RECUSADO("Recusado"),
    REEMBOLSADO("Reembolsado");
    
    private final String descricao;
    
    StatusPagamento(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}