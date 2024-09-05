package com.github.sardul3.io.api_best_practices_boot.idempotency.exceptions;

public class InvalidPaymentStateException extends RuntimeException {
    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
