package com.estacionamento_api.estacionamento.exception;


public class VeiculoNaoEncontradoException extends RuntimeException {
    public VeiculoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}