
package br.com.spbank.shared.application.exception;

public final class IdempotencyConflictException
        extends BusinessException {

    public IdempotencyConflictException() {
        super(
                "IDEMPOTENCY_CONFLICT",
                "request.idempotency-conflict"
        );
    }
}
