package com.github.sardul3.io.api_best_practices_boot.idempotency.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RedisConfig class provides the configuration for Redis integration with Spring Boot.
 * This configuration is necessary to interact with Redis, allowing us to perform operations such as storing and retrieving
 * idempotency keys and handling the state of payments. It defines the necessary beans to use Redis as a caching mechanism.
 * <p>
 * Redis plays a critical role in maintaining state and ensuring that our idempotency logic works efficiently by caching
 * the necessary data (e.g., payment statuses, idempotency keys).
 */
@Configuration
public class RedisConfig {
    /**
     * Provides a RedisTemplate bean for performing Redis operations with String keys and Object values.
     * RedisTemplate is a more flexible template that allows us to perform Redis operations like GET, SET, and DELETE
     * on objects of any type (i.e., String, Object). It helps to manage the caching of objects in Redis.
     *
     * @param connectionFactory RedisConnectionFactory used to create a connection to the Redis instance.
     * @return RedisTemplate configured to handle operations on String keys and Object values.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    /**
     * Provides a StringRedisTemplate bean specifically for operations involving String keys and String values.
     * This is particularly useful when we only need to store simple key-value pairs where both the key and the value
     * are strings (e.g., when using locks for handling concurrency).
     *
     * StringRedisTemplate simplifies the usage when dealing with only String-based key-value pairs, as it’s optimized for
     * these operations and provides a more convenient API for such cases.
     *
     * @param connectionFactory RedisConnectionFactory used to create a connection to the Redis instance.
     * @return StringRedisTemplate configured for operations with String keys and values.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
