package com.github.sardul3.io.api_best_practices_boot.eTags.controllers;

import com.github.sardul3.io.api_best_practices_boot.eTags.config.ETagGenerator;
import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.eTags.services.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing transactions and handling HTTP requests related to transactions.
 * This controller handles creating, retrieving, and updating transactions, along with managing caching and eTag generation.
 */
@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final CacheManager cacheManager;

    public TransactionController(TransactionService transactionService, CacheManager cacheManager) {
        this.transactionService = transactionService;
        this.cacheManager = cacheManager;
    }

    /**
     * Handles the creation of a new transaction.
     *
     * @param transaction the transaction details to create
     * @return a ResponseEntity containing the created transaction and HTTP status CREATED (201)
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        Transaction savedTransaction = transactionService.saveTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction);
    }

    /**
     * Retrieves a list of transactions, optionally filtered by 'from' and 'to' account.
     * This method also generates an eTag based on the content of the returned transactions to optimize subsequent requests.
     *
     * @param from the account initiating the transaction (optional)
     * @param to the account receiving the transaction (optional)
     * @param ifNoneMatch the eTag header sent by the client to check for resource modifications
     * @return a ResponseEntity containing the transactions and HTTP status OK (200), or NOT_MODIFIED (304) if the eTag matches
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false, defaultValue = "") String from,
            @RequestParam(required = false, defaultValue = "") String to,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        List<Transaction> transactions = transactionService.getAllTransactions(from, to);

        String eTag = ETagGenerator.generateETag(transactions);

        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(eTag).build();
        }

        return ResponseEntity.ok().eTag(eTag).body(transactions);
    }

    /**
     * Retrieves a specific transaction by its ID.
     * The method checks the cache for the transaction and generates a custom eTag based on the transaction's state.
     *
     * @param id the ID of the transaction to retrieve
     * @param ifNoneMatch the eTag header sent by the client to check for resource modifications
     * @return a ResponseEntity containing the transaction and HTTP status OK (200), or NOT_MODIFIED (304) if the eTag matches
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        Cache cache = cacheManager.getCache("transactionCache");
        if (cache != null && cache.get(id) != null) {
            // Cache hit, call your custom method
            log.info("Fetching transaction from the cache for transactionId: {}", id);
        }
        Optional<Transaction> transaction = transactionService.getTransactionById(id);

        if (transaction.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Generate a custom eTag using a hash based on transaction fields
        String eTag = ETagGenerator.generateETagForTransaction(transaction.get());

        // Always return cached data, regardless of ETag matching
        return ResponseEntity.ok().eTag(eTag).body(transaction.get());
    }

    /**
     * Updates the status of a transaction and returns the updated transaction.
     * This method also generates a new eTag for the updated transaction to reflect the changes.
     *
     * @param id the ID of the transaction to update
     * @param newStatus the new status to set for the transaction
     * @return a ResponseEntity containing the updated transaction and HTTP status OK (200)
     */
    @PutMapping("/{id}/status")
//    @CacheEvict(value = "transactionCache", key = "#id")
    public ResponseEntity<Transaction> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam Transaction.Status newStatus) {

        Transaction updatedTransaction = transactionService.updateTransactionStatus(id, newStatus);

        // Generate eTag for the updated transaction
        String eTag = ETagGenerator.generateETagForTransaction(updatedTransaction);

        return ResponseEntity.ok().eTag(eTag).body(updatedTransaction);
    }

}

