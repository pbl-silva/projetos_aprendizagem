package com.estacionamento_api.estacionamento.exception;

public class VeiculoNaoPermitidoException extends RuntimeException {
    public VeiculoNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}
