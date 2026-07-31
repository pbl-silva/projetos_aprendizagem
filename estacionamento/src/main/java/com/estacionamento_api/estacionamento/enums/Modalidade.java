package com.estacionamento_api.estacionamento.enums;

public enum Modalidade {
    DIARIA("Diária", 0.0),
    MENSAL("Mensal", 0.15);
    
    private final String descricao;
    private final double desconto;
    
    Modalidade(String descricao, double desconto) {
        this.descricao = descricao;
        this.desconto = desconto;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public double getDesconto() {
        return desconto;
    }
}