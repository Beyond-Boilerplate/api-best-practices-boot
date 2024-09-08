package com.github.sardul3.io.api_best_practices_boot.idempotency.controllers;

import com.github.sardul3.io.api_best_practices_boot.idempotency.exceptions.*;
import com.github.sardul3.io.api_best_practices_boot.idempotency.services.IdempotencyService;
import com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects.EndpointDescribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * The PaymentController handles the payment processing API and ensures that the same payment
 * is not processed multiple times using the idempotency mechanism. It manages requests
 * with idempotency keys, validates them, and stores transaction results in Redis.
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final IdempotencyService idempotencyService;

    public PaymentController(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    /**
     * Processes a payment request while ensuring idempotency.
     * The client must provide an idempotency key to prevent duplicate processing of the same request.
     * <p>
     * **Flow**:
     * - Validates the presence of an idempotency key.
     * - Acquires a lock to prevent concurrent payment processing.
     * - If the payment was already processed, returns the cached result.
     * - Otherwise, processes the payment and stores the result in Redis.
     * <p>
     * **Usage**: Use this endpoint to process a payment. Ensure the idempotency key is passed to avoid duplicate charges.
     *
     * @param idempotencyKey The unique idempotency key provided by the client.
     * @param paymentDetails The payment details (e.g., amount, currency).
     * @return A success response with the transaction ID, or the cached result if the request was repeated.
     */
    @EndpointDescribe("create new payment")
    @PostMapping("/payment")
    public ResponseEntity<String> processPayment(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                 @RequestBody Map<String, String> paymentDetails) {
        // Check if idempotency key is missing
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new MissingIdempotencyKeyException("Missing Idempotency-Key header.");
        }

        // Try to acquire lock to avoid race conditions
        if (!idempotencyService.acquireLock(idempotencyKey)) {
            throw new PaymentInProgressException("Payment is already being processed.");
        }

        try {
            // Check if the key already exists and has been used before
            Object existingValue = idempotencyService.getCachedResponse(idempotencyKey);
            if (existingValue == null) {
                throw new InvalidIdempotencyKeyException("Invalid or missing Idempotency-Key.");
            }

            if (!"KEY GENERATED".equals(existingValue)) {
                logger.info("Returning cached transaction result for Idempotency-Key: {}", idempotencyKey);
                return ResponseEntity.ok(existingValue.toString());
            }

            // Simulate payment processing (e.g., deduct money from account)
            String transactionId = processPaymentTransaction(paymentDetails);

            // Store the transaction result in Redis, replacing the placeholder
            idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "Transaction ID: " + transactionId);

            // Return success response with transaction ID
            return ResponseEntity.ok("Payment processed successfully with Transaction ID: " + transactionId);
        } finally {
            idempotencyService.releaseLock(idempotencyKey);
        }
    }


    /**
     * Retrieves the status of a payment using the idempotency key.
     * If the key exists and the payment has been processed, the status or transaction ID is returned.
     * <p>
     * **Usage**: This endpoint allows clients to check the status of a previously processed payment
     * using the idempotency key provided during the initial payment request.
     *
     * @param idempotencyKey The idempotency key associated with the payment request.
     * @return The status of the payment, or an error if no payment was found for the key.
     */
    @EndpointDescribe("get payment status")
    @GetMapping("/payment-status")
    public ResponseEntity<Object> getPaymentStatus(@RequestHeader("Idempotency-Key") String idempotencyKey) {
        // Retrieve the payment status from Redis
        Object paymentStatus = idempotencyService.getCachedResponse(idempotencyKey);
        if (paymentStatus == null || "KEY GENERATED".equals(paymentStatus)) {
            throw new PaymentNotFoundException("No payment found for this Idempotency-Key.");
        }
        return ResponseEntity.ok(paymentStatus);
    }

    /**
     * Simulates the actual payment processing logic.
     * In a real-world implementation, this would handle the logic for charging a user or completing the transaction.
     *
     * @param paymentDetails Details of the payment to be processed (e.g., amount, currency).
     * @return The generated transaction ID for the processed payment.
     */
    private String processPaymentTransaction(Map<String, String> paymentDetails) {
        // Dummy implementation of payment processing logic [ dummy transaction ID ]
        logger.info("Processing payment for details: {}", paymentDetails);
        return "TXN" + System.currentTimeMillis();
    }
}
