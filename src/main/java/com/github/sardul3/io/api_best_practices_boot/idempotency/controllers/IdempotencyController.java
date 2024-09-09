package com.github.sardul3.io.api_best_practices_boot.idempotency.controllers;

import com.github.sardul3.io.api_best_practices_boot.idempotency.services.IdempotencyService;
import com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects.EndpointDescribe;
import io.micrometer.observation.annotation.Observed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
/**
 * The IdempotencyController is responsible for generating server-side idempotency keys.
 * This key is required for all subsequent operations to ensure that the same request
 * is processed only once. It is a critical part of the idempotency pattern, which helps
 * prevent duplicate processing in distributed systems or when network issues cause retries.
 */
@RestController
@RequestMapping("/api")
@Observed
public class IdempotencyController {
    private final IdempotencyService idempotencyService;

    public IdempotencyController(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    /**
     * Generates a unique idempotency key using a UUID and stores it in Redis.
     * The generated key can be used for ensuring that identical requests with the same key
     * do not result in duplicate operations.
     * <p>
     * **Usage**: Clients should call this endpoint to generate an idempotency key
     * before making subsequent requests that require idempotency (e.g., payment processing).
     *
     * @return ResponseEntity containing the generated idempotency key.
     */
    @EndpointDescribe("generate idempotency key")
    @GetMapping("server-generated/generate-key")
    public ResponseEntity<String> generateIdempotencyKey() {
        String idempotencyKey = UUID.randomUUID().toString();
        // Store the generated key in Redis with an expiration
        idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "KEY GENERATED");
        return ResponseEntity.ok(idempotencyKey);
    }
}
