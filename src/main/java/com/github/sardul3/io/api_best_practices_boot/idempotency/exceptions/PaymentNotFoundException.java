package com.github.sardul3.io.api_best_practices_boot.idempotency.exceptions;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
