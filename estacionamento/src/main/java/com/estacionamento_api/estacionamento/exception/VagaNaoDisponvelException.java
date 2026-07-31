package com.estacionamento_api.estacionamento.exception;

public class VagaNaoDisponvelException extends RuntimeException {
    public VagaNaoDisponvelException(String mensagem) {
        super(mensagem);
    }
}