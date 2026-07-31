package com.estacionamento_api.estacionamento.enums;

public enum TipoVaga {
    COMUM("Vaga Comum", 23),
    PCD("Vaga PCD", 15),
    MOTO("Vaga Moto", 10),
    ELETRICA("Vaga Elétrica", 2);
    
    private final String descricao;
    private final int quantidade;
    
    TipoVaga(String descricao, int quantidade) {
        this.descricao = descricao;
        this.quantidade = quantidade;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public int getQuantidade() {
        return quantidade;
    }
}