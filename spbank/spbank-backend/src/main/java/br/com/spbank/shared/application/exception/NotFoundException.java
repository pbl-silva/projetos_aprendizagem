package br.com.spbank.shared.application.exception;

public final class NotFoundException extends BusinessException {

    public NotFoundException(String code) {
        super(code, "resource.not-found");
    }
}