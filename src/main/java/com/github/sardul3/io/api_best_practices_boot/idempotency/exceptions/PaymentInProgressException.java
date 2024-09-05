package com.github.sardul3.io.api_best_practices_boot.idempotency.exceptions;

public class PaymentInProgressException extends RuntimeException {
    public PaymentInProgressException(String message) {
        super(message);
    }
}
