package com.github.sardul3.io.api_best_practices_boot.idempotency.models;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED;

    // Validate if the transition is valid
    public boolean isValidTransition(PaymentStatus newState) {
        switch (this) {
            case PENDING:
                return newState == PROCESSING; // Can only move to PROCESSING from PENDING
            case PROCESSING:
                return newState == COMPLETED; // Can only move to COMPLETED from PROCESSING
            case COMPLETED:
                return false; // Cannot move out of COMPLETED state
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}

