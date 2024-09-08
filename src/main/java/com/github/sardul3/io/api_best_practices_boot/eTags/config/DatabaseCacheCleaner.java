package com.github.sardul3.io.api_best_practices_boot.eTags.config;

import com.github.sardul3.io.api_best_practices_boot.eTags.repos.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Component responsible for clearing the transaction cache at application startup
 * if the underlying database contains no transaction records.
 * <p>
 * This ensures that the cache doesn't contain stale or orphaned entries when the
 * database is empty, preventing unnecessary cache hits and potential performance issues.
 * </p>
 */
@Component
@Slf4j
public class DatabaseCacheCleaner {
    private final TransactionRepository transactionRepository;
    private final CacheManager cacheManager;

    public DatabaseCacheCleaner(TransactionRepository transactionRepository, CacheManager cacheManager) {
        this.transactionRepository = transactionRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Validates the cache on application startup by checking if the database has any transaction records.
     * If the database is empty, the method clears the "transactionCache" to prevent stale data from being cached.
     * <p>
     * This method runs automatically after dependency injection is complete, ensuring that
     * the cache is in sync with the state of the database at startup.
     * </p>
     */
    @PostConstruct
    public void validateCache() {
        long transactionCount = transactionRepository.count();
        if (transactionCount == 0) {
            // Database is empty, clear the cache
            cacheManager.getCache("transactionCache").clear();
            cacheManager.getCache("transactionsCache").clear();
            cacheManager.getCache("transactionsPFSCache").clear();

            log.warn("Cache cleared as the database is empty on application startup");
        }
    }
}
