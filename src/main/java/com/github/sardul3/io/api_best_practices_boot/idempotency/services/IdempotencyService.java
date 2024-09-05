package com.github.sardul3.io.api_best_practices_boot.idempotency.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * IdempotencyService provides methods to manage idempotency keys and handle concurrency using Redis as the caching mechanism.
 * This service ensures that repeated operations with the same idempotency key do not result in duplicate processing and provides locking mechanisms
 * to handle concurrent requests for the same operation safely.
 * <p>
 * The service leverages two main Redis templates:
 *  - RedisTemplate for storing and updating complex objects with idempotency keys.
 *  - StringRedisTemplate for managing simple key-value pairs, such as locks to avoid race conditions.
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${idempotency.cache-expiration}")
    private long cacheExpiration;

    @Value("${idempotency.lock-expiration}")
    private long lockExpiration;

    private static final String LOCK_PREFIX = "lock_";

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Checks if an idempotency key exists, and if it does, updates the value (such as a processed payment status).
     * If the key does not exist, it sets the key with an initial value and expiration time.
     * <p>
     * **Usage**: This method is typically called after receiving a request with an idempotency key.
     * If the key is already present, it may indicate that the operation has already been processed and only needs to update the result.
     * If the key is not present, it will be initialized with the provided value (e.g., "KEY GENERATED").
     *
     * @param key   The idempotency key (typically generated by the server or client).
     * @param data  The data to be associated with the key (e.g., "Transaction ID: XYZ").
     * @return      true if the key was newly created, false if the key was updated.
     */
    public boolean checkAndSetIdempotencyKey(String key, Object data) {
        logger.debug("Checking if idempotency key exists: {}", key);

        // Check if the key already exists
        Object existingValue = redisTemplate.opsForValue().get(key);

        if (existingValue != null) {
            logger.info("Updating the existing idempotency key: {} with new value: {}", key, data);
            // Update the existing key with the new data (e.g., "Processed", "Transaction ID: XYZ")
            redisTemplate.opsForValue().set(key, data, cacheExpiration, TimeUnit.SECONDS); // Also ensure to reset the expiration
            return false; // Return false since the key already existed and we updated it
        }

        // If the key doesn't exist, set the key with the initial value and expiration time
        logger.info("Setting new idempotency key: {} with initial value: {}", key, data);
        redisTemplate.opsForValue().set(key, data, cacheExpiration, TimeUnit.SECONDS);
        return true; // Return true since we set the key for the first time
    }

    /**
     * Retrieves a cached response associated with a given idempotency key.
     * <p>
     * **Usage**: This method is called when the server needs to check the result of a previously processed request, ensuring that duplicate operations
     * are not performed. If the key exists, it returns the associated value, such as a transaction result.
     *
     * @param key  The idempotency key to look up in Redis.
     * @return     The cached response if present, or null if the key does not exist.
     */
    public Object getCachedResponse(String key) {
        logger.debug("Retrieving cached response for key: {}", key);
        Object response = redisTemplate.opsForValue().get(key);
        if (response != null) {
            logger.info("Found cached response for key: {}", key);
        } else {
            logger.warn("No cached response found for key: {}", key);
        }
        return response;
    }

    /**
     * Attempts to acquire a lock for the given idempotency key to prevent concurrent requests from processing the same operation.
     * The lock is time-limited to prevent deadlock situations.
     * <p>
     * **Usage**: This method is invoked when handling concurrent operations (e.g., payment processing) to ensure that only one process can handle a
     * given idempotency key at a time. Locks are usually set before processing begins and released once the operation is complete.
     *
     * @param key  The idempotency key to lock.
     * @return     true if the lock was successfully acquired, false if the lock already exists.
     */
    public boolean acquireLock(String key) {
        logger.debug("Attempting to acquire lock for key: {}", key);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + key, "LOCKED", lockExpiration, TimeUnit.SECONDS);
        if (success != null && success) {
            logger.info("Successfully acquired lock for key: {}", key);
        } else {
            logger.warn("Failed to acquire lock for key: {}", key);
        }
        return success != null && success;
    }

    /**
     * Releases the lock associated with the given idempotency key, allowing other requests to proceed.
     * <p>
     * **Usage**: This method is called once the operation associated with the idempotency key is completed to release the lock,
     * allowing other processes to continue or retry if necessary.
     *
     * @param key  The idempotency key whose lock should be released.
     */
    public void releaseLock(String key) {
        logger.debug("Releasing lock for key: {}", key);
        stringRedisTemplate.delete(LOCK_PREFIX + key);
        logger.info("Lock released for key: {}", key);    }

    /**
     * Deletes or evicts the cache associated with a specific idempotency key.
     * <p>
     * **Usage**: This method is used to remove stale or no longer needed cache entries. This can be triggered after the result of an operation is no longer
     * relevant or when manually evicting data to free up cache space.
     *
     * @param key  The idempotency key whose cache entry should be deleted.
     */
    public void evictCache(String key) {
        redisTemplate.delete(key);
    }
}

