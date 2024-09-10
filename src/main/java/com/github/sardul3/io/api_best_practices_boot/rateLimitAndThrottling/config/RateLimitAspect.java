package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Aspect responsible for applying rate limiting to methods annotated with {@link RateLimit}.
 *
 * <p>This class intercepts methods annotated with {@link RateLimit} and tracks the number of
 * requests made by a client (based on their IP address) to the target endpoint. If the number
 * of requests exceeds the specified limit within the defined time window, the client will receive
 * an HTTP 429 (Too Many Requests) response.</p>
 *
 * <p>The rate limit can either be set directly via the {@link RateLimit} annotation or defined in
 * external configuration (e.g., application.yml). Redis is used to store the request count per client,
 * identified by their IP address and the target HTTP method.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *     <li>Uses Redis for distributed rate limiting.</li>
 *     <li>Supports configurable rate limits and durations on a per-endpoint and per-method basis.</li>
 *     <li>Provides IP-based request tracking.</li>
 *     <li>Returns the "Retry-After" header in 429 responses, indicating when the client can retry.</li>
 * </ul>
 *
 * <p><strong>Usage Considerations:</strong></p>
 * <ul>
 *     <li>This implementation uses the client's IP address for tracking requests, which may not be suitable
 *     in cases where clients are behind proxies or using NAT. For production environments, it is recommended
 *     to configure X-Forwarded-For headers or similar mechanisms to accurately identify the client.</li>
 * </ul>
 */
@Aspect
@Component
public class RateLimitAspect {
    public static final String ERROR_MESSAGE = "Too many requests to %s [%s] from IP %s! Please try again after %d seconds!";
    private final ConcurrentHashMap<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

    @Value("${app.rate.limit:#{200}}")
    private int rateLimit;

    @Value("${APP_RATE_DURATIONINMS:#{60000}}")
    private long rateDuration;

    private final RedisTemplate<String, String> redisTemplate;
    private final Environment environment;

    public RateLimitAspect(RedisTemplate<String, String> redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    /**
     * Intercepts methods annotated with {@link RateLimit} and checks if the number of requests
     * from a client IP has exceeded the allowed rate limit within the specified time window.
     *
     * <p>If the rate limit is exceeded, a {@link RateLimitExceededException} is thrown, and the client
     * receives a 429 Too Many Requests response along with a "Retry-After" header indicating when
     * they can send the next request.</p>
     *
     * @param joinPoint the join point of the intercepted method.
     * @param rateLimit the rate limit annotation applied to the method.
     * @return the result of the intercepted method execution, if the rate limit is not exceeded.
     * @throws Throwable if the rate limit is exceeded or any other error occurs.
     */
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String clientIp = request.getRemoteAddr();
        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();
        String redisKey = "rate_limit:" + clientIp + ":" + requestUri + ":" + httpMethod;

        // Resolve rate limit and duration from annotation or configuration
        int limit = resolveRateLimit(requestUri, httpMethod, rateLimit.limit());
        long duration = resolveRateDuration(requestUri, httpMethod, rateLimit.duration());

        Long currentRequestCount = redisTemplate.opsForValue().increment(redisKey);
        if (currentRequestCount == null) {
            currentRequestCount = 1L;
            redisTemplate.opsForValue().set(redisKey, String.valueOf(currentRequestCount));
            redisTemplate.expire(redisKey, duration, TimeUnit.MILLISECONDS);
        }

        if (currentRequestCount == 1) {
            redisTemplate.expire(redisKey, duration, TimeUnit.MILLISECONDS);
        }

        if (currentRequestCount > limit) {
            long retryAfterSeconds = duration / 1000;
            throw new RateLimitExceededException(String.format(ERROR_MESSAGE, requestUri, httpMethod, clientIp, retryAfterSeconds), retryAfterSeconds);
        }

        return joinPoint.proceed(); // Proceed with the method execution
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
