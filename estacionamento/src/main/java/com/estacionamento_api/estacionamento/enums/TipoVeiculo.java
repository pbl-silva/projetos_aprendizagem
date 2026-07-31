package com.estacionamento_api.estacionamento.enums;

public enum TipoVeiculo {
    CARRO("Carro", 20.00),
    MOTO("Moto", 15.00),
    CARRO_ELETRICO("Carro Elétrico", 19.00);
    
    private final String descricao;
    private final double precoDiaria;
    
    TipoVeiculo(String descricao, double precoDiaria) {
        this.descricao = descricao;
        this.precoDiaria = precoDiaria;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public double getPrecoDiaria() {
        return precoDiaria;
    }
}