package com.estacionamento_api.estacionamento.enums;

public enum TipoVeiculo {
    CARRO(20.00),
    MOTO(15.00),
    CARRO_ELETRICO(19.00);
    
    private final double precoHora;
    
    TipoVeiculo(double precoHora) {
        this.precoHora = precoHora;
    }
    
    public double getPrecoHora() {
        return precoHora;
    }
}
