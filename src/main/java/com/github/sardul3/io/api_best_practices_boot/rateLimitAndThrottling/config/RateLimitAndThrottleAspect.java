package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Aspect
@Component
@Slf4j
public class RateLimitAndThrottleAspect {
    public static final String ERROR_MESSAGE = "Too many requests to %s [%s] from IP %s! Please try again after %d seconds!";
    private final ConcurrentHashMap<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

    final Supplier<BucketConfiguration> bucketConfiguration;

    final ProxyManager<String> proxyManager;    private final RedisTemplate<String, String> redisTemplate;
    final RedisBucketLimitAndThrottleConfig config;

    private final Environment environment;

    public RateLimitAndThrottleAspect(Supplier<BucketConfiguration> bucketConfiguration, ProxyManager<String> proxyManager, RedisTemplate<String, String> redisTemplate, RedisBucketLimitAndThrottleConfig config, Environment environment) {
        this.bucketConfiguration = bucketConfiguration;
        this.proxyManager = proxyManager;
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.environment = environment;
    }

    @Around("@annotation(rateLimitAndThrottle)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimitAndThrottle rateLimitAndThrottle) throws Throwable {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String clientIp = request.getRemoteAddr();
        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();
        requestUri = requestUri.replace("/", "").trim();

        String key = "rate_limit:" + clientIp + ":" + requestUri + ":" + httpMethod;

        // Resolve the bucket from Redis
        String finalRequestUri = requestUri;
        Bucket bucket = proxyManager.builder().build(key, () -> BucketConfiguration.builder()
                .addLimit(config.resolveBandwidth(finalRequestUri, httpMethod))
                .build());

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Get the total capacity and the remaining tokens
        long remainingTokens = probe.getRemainingTokens();
        long totalCapacity = bucket.getAvailableTokens();

        // Calculate the 80% threshold
        long threshold = (long) (totalCapacity * 0.7);

        log.info(">>>>>>>>remainingTokens: {}", remainingTokens);

        // If more than 80% of tokens are used, throttle using Bucket4J's native mechanisms
        if (remainingTokens <= threshold) {
            log.info("Throttling active, remaining tokens: {}, threshold: {}", remainingTokens, threshold);

            // If required, wait until tokens are available before proceeding
            long waitForRefillNanos = bucket.getAvailableTokens() == 0 ? probe.getNanosToWaitForRefill() : 0;

            if (waitForRefillNanos > 0) {
                log.info("Waiting for tokens to refill for {} nanos", waitForRefillNanos);
                // Wait until a token becomes available
                Thread.sleep(TimeUnit.NANOSECONDS.toMillis(waitForRefillNanos));
            }
        }

        // If tokens are consumed, proceed
        if (probe.isConsumed()) {
            return joinPoint.proceed();
        } else {
            // No tokens left, throw RateLimitExceededException with Retry-After header
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            throw new RateLimitExceededException(String.format(ERROR_MESSAGE, requestUri, httpMethod, clientIp, retryAfterSeconds), retryAfterSeconds);
        }
    }


    /**
     * Resolve rate limit for the given endpoint and HTTP method from the configuration or annotation.
     *
     * @param endpoint   the requested URI
     * @param method     the HTTP method (GET, POST, etc.)
     * @param annotationLimit the limit specified in the annotation (-1 means use config)
     * @return the rate limit value
     */
    private int resolveRateLimit(String endpoint, String method, int annotationLimit) {
        if (annotationLimit > 0) {
            return annotationLimit; // Use the limit from the annotation if provided
        }

        String configLimit = environment.getProperty("rate-limits.endpoints." + endpoint + "." + method + ".limit");
        return configLimit != null ? Integer.parseInt(configLimit) : environment.getProperty("rate-limits.default.limit", Integer.class, 200);
    }

    /**
     * Resolve rate duration for the given endpoint and HTTP method from the configuration or annotation.
     *
     * @param endpoint   the requested URI
     * @param method     the HTTP method (GET, POST, etc.)
     * @param annotationDuration the duration specified in the annotation (-1 means use config)
     * @return the rate duration value
     */
    private long resolveRateDuration(String endpoint, String method, long annotationDuration) {
        if (annotationDuration > 0) {
            return annotationDuration; // Use the duration from the annotation if provided
        }

        String configDuration = environment.getProperty("rate-limits.endpoints." + endpoint + "." + method + ".duration");
        return configDuration != null ? Long.parseLong(configDuration) : environment.getProperty("rate-limits.default.duration", Long.class, 60000L);
    }
}
