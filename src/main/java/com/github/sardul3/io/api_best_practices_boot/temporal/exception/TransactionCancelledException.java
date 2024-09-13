package com.github.sardul3.io.api_best_practices_boot.temporal.exception;

public class TransactionCancelledException extends RuntimeException {
    public TransactionCancelledException() {
        super("Transaction was cancelled");
    }
}
