package br.com.spbank.conta.application.exception;

import br.com.spbank.shared.application.exception.BusinessException;

public final class AccountInactiveException extends BusinessException {

    public AccountInactiveException() {
        super("ACCOUNT_INACTIVE", "account.inactive");
    }
}