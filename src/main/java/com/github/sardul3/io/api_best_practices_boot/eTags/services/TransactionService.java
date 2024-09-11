package com.github.sardul3.io.api_best_practices_boot.eTags.services;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.eTags.repos.TransactionRepository;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.caching.TransactionCacheService;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering.FilterCriteria;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering.TransactionSpecificationBuilder;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.model.PaginatedTransaction;
import io.micrometer.observation.annotation.Observed;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

/**
 * Service class responsible for handling transaction-related business logic.
 * This service manages operations such as retrieving, creating, updating, and deleting transactions.
 * It also handles caching and cache eviction for performance optimization.
 */
@Service
@Slf4j
public class TransactionService {


    private final TransactionRepository transactionRepository;
    private final TransactionCacheService transactionCacheService;
    public TransactionService(TransactionRepository transactionRepository, TransactionCacheService transactionCacheService) {
        this.transactionRepository = transactionRepository;
        this.transactionCacheService = transactionCacheService;
    }

    /**
     * Retrieves a list of transactions filtered by 'from' and 'to' accounts.
     * If no filters are provided, it returns all transactions.
     *
     * @param from the account initiating the transaction (optional)
     * @param to the account receiving the transaction (optional)
     * @return a list of transactions based on the specified filters
     */
    @Cacheable(value = "transactionsCache", key = "#from + '_' + #to", unless = "#result == null || #result.isEmpty()")
    public List<Transaction> getAllTransactions(String from, String to) {
        if (!from.isEmpty() && !to.isEmpty()) {
            return transactionRepository.findByFromAccountAndToAccount(from, to);
        } else if (!from.isEmpty()) {
            return transactionRepository.findByFromAccount(from);
        } else if (!to.isEmpty()) {
            return transactionRepository.findByToAccount(to);
        }
        return transactionRepository.findAll();
    }

    @Observed(name = "transactions.all.db",
            contextualName = "getting-all-transactions",
            lowCardinalityKeyValues = {"GET", "transactions"})
    public Page<Transaction> getAllTransactionsWithPage(List<FilterCriteria> filters, Pageable pageable) {
        // Now we call the cached method from another service, going through the proxy
        PaginatedTransaction transactions = transactionCacheService.getAllTransactionsWithCache(filters, pageable);

        return getPaginatedTransactions(transactions, pageable);
    }

    @Observed(name = "transactions.all.page",
            contextualName = "creating-page-all-transactions",
            lowCardinalityKeyValues = {"GET", "transactions"})
    public Page<Transaction> getPaginatedTransactions(PaginatedTransaction transactions, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), transactions.getTransactions().size());
        List<Transaction> paginatedList = transactions.getTransactions().subList(start, end);

        return new PageImpl<>(paginatedList, pageable, transactions.getTotal());
    }

    /**
     * Retrieves a specific transaction by its ID. The result is cached to avoid repeated database lookups.
     * <p>
     * If the transaction is present in the cache, it will be fetched from the cache; otherwise, the database is queried.
     * </p>
     *
     * @param transactionId the ID of the transaction to retrieve
     * @return an Optional containing the transaction, or an empty Optional if no transaction is found
     */
    @Cacheable(value = "transactionCache", key = "#transactionId", unless = "#result == null")
    public Optional<Transaction> getTransactionById(Long transactionId) {
        log.info("Fetching transaction from the database for transactionId: {}", transactionId);
        return transactionRepository.findById(transactionId);
    }

    /**
     * Saves a new transaction to the database.
     *
     * @param transaction the transaction details to save
     * @return the saved transaction
     */
    @Caching(evict = {
            @CacheEvict(value = "transactionsCache", allEntries = true),  // Evict all entries from transactionsCache
            @CacheEvict(value = "transactionsPFSCache", allEntries = true),  // Evict all entries from transactionsCache
    })
    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    /**
     * Updates the status of an existing transaction and stores the updated transaction in the cache.
     * <p>
     * This method uses {@link CachePut} to ensure that the cache is updated with the new transaction status after the update.
     * </p>
     *
     * @param transactionId the ID of the transaction to update
     * @param newStatus the new status to set for the transaction
     * @return the updated transaction
     * @throws EntityNotFoundException if the transaction with the specified ID is not found
     */
    @Caching(evict = {
            @CacheEvict(value = "transactionsCache", allEntries = true),
            @CacheEvict(value = "transactionsPFSCache", allEntries = true),
    })
    @CachePut(value = "transactionCache", key = "#transactionId")
    public Transaction updateTransactionStatus(Long transactionId, Transaction.Status newStatus) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);

        if (transactionOpt.isEmpty()) {
            throw new EntityNotFoundException("Transaction not found for id: " + transactionId);
        }

        Transaction transaction = transactionOpt.get();
        transaction.setStatus(newStatus);

        // Save the updated transaction to the database
        return transactionRepository.save(transaction);
    }

    /**
     * Deletes a specific transaction by its ID and evicts the corresponding cache entry.
     * <p>
     * The {@link CacheEvict} annotation ensures that the cache entry for the deleted transaction is removed.
     * </p>
     *
     * @param transactionId the ID of the transaction to delete
     */
    @Caching(evict = {
            @CacheEvict(value = "transactionsCache", allEntries = true),  // Evict all entries from transactionsCache
            @CacheEvict(value = "transactionsPFSCache", allEntries = true),  // Evict all entries from transactionsCache
            @CacheEvict(value = "transactionCache", key = "#transactionId")  // Evict the specific entry from transactionCache
    })
    public void deleteTransaction(Long transactionId) {
        transactionRepository.deleteById(transactionId);
    }

    /**
     * Deletes all transactions from the database and evicts all cache entries.
     * <p>
     * The {@link CacheEvict} annotation with {@code allEntries = true} ensures that all cached transaction entries are cleared.
     * </p>
     */
    @Caching(evict = {
            @CacheEvict(value = "transactionsCache", allEntries = true),  // Evict all entries from transactionsCache
            @CacheEvict(value = "transactionsPFSCache", allEntries = true),  // Evict all entries from transactionsCache
            @CacheEvict(value = "transactionCache", allEntries = true)  // Evict all transactionCache
    })    public void deleteAllTransactions() {
        transactionRepository.deleteAll();
    }


}

