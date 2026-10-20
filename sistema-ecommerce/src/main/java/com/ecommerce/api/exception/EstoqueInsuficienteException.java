package com.ecommerce.api.exception;

public class EstoqueInsuficienteException extends IllegalArgumentException {
    public EstoqueInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
