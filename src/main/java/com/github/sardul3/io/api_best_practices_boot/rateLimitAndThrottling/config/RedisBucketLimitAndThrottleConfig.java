package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
@Slf4j
public class RedisBucketLimitAndThrottleConfig {

    private final Environment environment;

    @Value("${rate-limits-bucket.default.bucket-size}")
    private int defaultBucketSize;

    @Value("${rate-limits-bucket.default.refill-duration}")
    private int defaultRefillDuration;

    @Value("${rate-limits-bucket.default.refill-tokens}")
    private int defaultRefillTokens;

    public RedisBucketLimitAndThrottleConfig(Environment environment) {
        this.environment = environment;
    }

    private RedisClient redisClient() {
        return RedisClient.create(RedisURI.builder()
                .withHost("localhost")
                .withPort(6379)
                .withSsl(false)
                .build());
    }

    @Bean
    public ProxyManager<String> lettuceBasedProxyManager() {
        RedisClient redisClient = redisClient();
        StatefulRedisConnection<String, byte[]> redisConnection = redisClient
                .connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return LettuceBasedProxyManager.builderFor(redisConnection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1L)))
                .build();
    }

    @Bean
    public Supplier<BucketConfiguration> bucketConfiguration() {
        return () -> BucketConfiguration.builder()
                .addLimit(resolveBandwidth("default", "get"))
                .build();
    }

    Bandwidth resolveBandwidth(String endpoint, String method) {
        endpoint = endpoint.replace("/", "").trim();
        String path = String.format("rate-limits-bucket.endpoints.%s.%s", endpoint, method.toLowerCase());
        log.info("path" + path);
        // Resolve bucket size, refill tokens, and refill duration dynamically
        int bucketSize = environment.getProperty(path + ".bucket-size", Integer.class, defaultBucketSize);
        int refillTokens = environment.getProperty(path + ".refill-tokens", Integer.class, defaultRefillDuration);
        long refillDuration = environment.getProperty(path + ".refill-duration", Integer.class, defaultRefillTokens);  // In seconds

        return Bandwidth.simple(refillTokens, Duration.ofSeconds(refillDuration)).withInitialTokens(bucketSize);
    }
}
