package com.github.sardul3.io.api_best_practices_boot.idempotency.exceptions;

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
