package com.github.sardul3.io.api_best_practices_boot.idempotency.controllers;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * RedisHealthCheckController provides endpoints for checking the health of Redis,
 * retrieving values by key, and gathering Redis statistics like cache size and last eviction time.
 */
@RestController
@RequestMapping("/api/redis")
public class RedisHealthCheckController {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthCheckController(StringRedisTemplate stringRedisTemplate, RedisConnectionFactory redisConnectionFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * Checks the health of the Redis connection by sending a PING command.
     * If the connection is healthy, Redis responds with "PONG".
     * <p>
     * **Usage**: Call this endpoint to ensure the application can successfully connect to Redis.
     * It is especially useful for health checks in production environments.
     *
     * @return ResponseEntity indicating whether the connection to Redis is successful or failed.
     */
    @GetMapping("/health")
    public ResponseEntity<String> checkRedisConnection() {
        try {
            // Pinging Redis to check connection
            String result = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection().ping();
            if ("PONG".equals(result)) {
                return ResponseEntity.ok("Successfully connected to Redis.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Redis response: " + result);
            }
        } catch (Exception e) {
            // Catching any exceptions related to the Redis connection
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect to Redis: " + e.getMessage());
        }
    }

    /**
     * Retrieves the value associated with a specific Redis key.
     *
     * **Usage**: Call this endpoint to get the value for a given Redis key. If the key does not exist,
     * the response will indicate the key was not found.
     *
     * @param key The Redis key whose value is to be retrieved.
     * @return ResponseEntity containing the value associated with the key, or a not found message.
     */
    @GetMapping("/{key}")
    public ResponseEntity<String> getValueForKey(@PathVariable String key) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value != null) {
                return ResponseEntity.ok("Value for key '" + key + "': " + value);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Key '" + key + "' not found in Redis.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving key '" + key + "': " + e.getMessage());
        }
    }

    /**
     * Retrieves Redis statistics including the total number of keys in the cache,
     * the time since the last eviction was run, and other Redis stats.
     *
     * **Usage**: Call this endpoint to get an overview of the Redis cache health and performance metrics.
     *
     * @return ResponseEntity containing Redis stats such as total keys and time since last eviction.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRedisStats() {
        try {
            // Retrieve Redis stats as Properties
            Properties infoProperties = redisConnectionFactory.getConnection().info();

            // Convert Properties to Map<String, Object>
            Map<String, Object> stats = new HashMap<>();
            for (Map.Entry<Object, Object> entry : infoProperties.entrySet()) {
                stats.put(entry.getKey().toString(), entry.getValue());
            }

            // Add total keys in Redis
            long totalKeys = stringRedisTemplate.getConnectionFactory().getConnection().dbSize();
            stats.put("totalKeys", totalKeys);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error retrieving Redis stats: " + e.getMessage()));
        }
    }
}
