package com.github.sardul3.io.api_best_practices_boot.eTags.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * Configuration class for customizing Redis caching in the application.
 * <p>
 * This class enables caching and configures Redis to serialize cache values using
 * Jackson's JSON serializer for better readability and compatibility across different platforms.
 * </p>
 */
@Configuration
@EnableCaching
public class RedisDataFormatConfig {

    /**
     * Configures the Redis cache to use JSON serialization for cache values.
     * <p>
     * The use of {@link GenericJackson2JsonRedisSerializer} ensures that the cache data is
     * stored in a readable and structured format, making it easier to debug and maintain compatibility.
     * </p>
     *
     * @return the RedisCacheConfiguration with JSON serialization for cache values
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // Set the TTL to 3 minutes (180 seconds)
                .entryTtl(Duration.ofMinutes(3))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}

