package br.com.spbank.conta.application.exception;

import br.com.spbank.shared.application.exception.BusinessException;

public final class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException() {
        super("INSUFFICIENT_FUNDS", "account.insufficient-funds");
    }
}