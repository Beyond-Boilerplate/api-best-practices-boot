package com.github.sardul3.io.api_best_practices_boot.idempotency.exceptions;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException(String message) {
        super(message);
    }
}
