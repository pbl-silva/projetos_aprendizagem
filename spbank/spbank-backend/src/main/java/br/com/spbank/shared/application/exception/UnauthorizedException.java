package br.com.spbank.shared.application.exception;

public final class UnauthorizedException
        extends BusinessException {

    public UnauthorizedException() {
        super(
                "AUTHENTICATION_REQUIRED",
                "auth.invalid-credentials"
        );
    }
}