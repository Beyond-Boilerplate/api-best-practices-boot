package com.github.sardul3.io.api_best_practices_boot.eTags.controllers;

import com.github.sardul3.io.api_best_practices_boot.eTags.config.ETagGenerator;
import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.eTags.services.TransactionService;
import com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects.EndpointDescribe;
import io.micrometer.observation.annotation.Observed;
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
@Observed
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
    @EndpointDescribe("create a new transaction")
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        log.debug("Saving a new transaction fromAccount={}, toAccount={}, amount={}",
                transaction.getFromAccount(), transaction.getToAccount(), transaction.getAmount());
        Transaction savedTransaction = transactionService.saveTransaction(transaction);
        log.debug("Transaction saved successfully with ID: {}", savedTransaction.getTransactionId());
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
    @EndpointDescribe("get all transactions (v1)")
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false, defaultValue = "") String from,
            @RequestParam(required = false, defaultValue = "") String to,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        log.debug("Fetching transactions with optional filters: fromAccount={}, toAccount={}", from, to);
        List<Transaction> transactions = transactionService.getAllTransactions(from, to);
        log.debug("Fetched {} transactions from the database", transactions.size());
        String eTag = ETagGenerator.generateETag(transactions);
        log.debug("Generated eTag for the transactions list: {}", eTag);
        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            log.debug("eTag match found. Returning 304 NOT_MODIFIED");
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
    @EndpointDescribe("get details on a individual transaction")
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        log.debug("Fetching transaction with ID: {}", id);
        Cache cache = cacheManager.getCache("transactionCache");
        if (cache != null && cache.get(id) != null) {
            log.debug("Cache hit for transactionId: {}", id);
        } else {
            log.debug("Cache miss for transactionId: {}", id);
        }
        Optional<Transaction> transaction = transactionService.getTransactionById(id);

        if (transaction.isEmpty()) {
            log.warn("Transaction with ID {} not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        // Generate a custom eTag using a hash based on transaction fields
        String eTag = ETagGenerator.generateETagForTransaction(transaction.get());
        log.debug("Generated eTag for transaction with ID {}: {}", id, eTag);

        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            log.debug("eTag match found for transaction with ID {}. Returning 304 NOT_MODIFIED", id);
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(eTag).build();
        }
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
    @EndpointDescribe("update a transaction")
    @PutMapping("/{id}/status")
    public ResponseEntity<Transaction> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam Transaction.Status newStatus) {
        log.debug("Updating status of transaction ID: {} to {}", id, newStatus);
        Transaction updatedTransaction = transactionService.updateTransactionStatus(id, newStatus);
        log.debug("Transaction ID {} status updated to {}", id, newStatus);
        // Generate eTag for the updated transaction
        String eTag = ETagGenerator.generateETagForTransaction(updatedTransaction);
        log.debug("Generated eTag for updated transaction with ID {}: {}", id, eTag);

        return ResponseEntity.ok().eTag(eTag).body(updatedTransaction);
    }

}

