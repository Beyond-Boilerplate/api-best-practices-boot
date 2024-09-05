package com.github.sardul3.io.api_best_practices_boot.idempotency.exceptions;

public class MissingIdempotencyKeyException extends RuntimeException {
    public MissingIdempotencyKeyException(String message) {
        super(message);
    }
}
