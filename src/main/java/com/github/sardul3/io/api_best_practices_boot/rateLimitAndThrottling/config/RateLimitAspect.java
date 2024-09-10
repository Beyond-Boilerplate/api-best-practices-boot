package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception.RateLimitExceededException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Aspect responsible for enforcing rate limiting on methods annotated with {@link RateLimit}.
 *
 * <p>This class intercepts methods annotated with {@link RateLimit} and tracks the number
 * of requests from each client IP address. If the number of requests from a particular IP
 * exceeds the defined limit within a certain time window, a {@link RateLimitExceededException}
 * is thrown.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *     <li>Simple, in-memory request tracking using a {@link ConcurrentHashMap}.</li>
 *     <li>IP-based identification of clients. This means requests from the same IP are
 *     counted together.</li>
 *     <li>Requests older than the specified duration (e.g., 60 seconds) are automatically
 *     forgotten.</li>
 *     <li>If a client exceeds the allowed number of requests within the specified time window,
 *     an error response (HTTP 429 Too Many Requests) is returned.</li>
 * </ul>
 *
 * <p><strong>Important Considerations:</strong></p>
 * <ul>
 *     <li>This is a basic implementation meant for local use and testing. For production
 *     systems, consider more sophisticated rate limiting techniques like Redis-based
 *     distributed rate limiting, request prioritization, or token bucket algorithms.</li>
 *     <li>IP-based identification may not work well in cases where clients are behind
 *     proxies or network address translation (NAT).</li>
 * </ul>
 */
@Aspect
@Component
public class RateLimitAspect {
    public static final String ERROR_MESSAGE = "To many request at endpoint %s from IP %s! Please try again after %d milliseconds!";
    private final ConcurrentHashMap<String, List<Long>> requestCounts = new ConcurrentHashMap<>();

    @Value("${app.rate.limit:#{200}}")
    private int rateLimit;

    @Value("${APP_RATE_DURATIONINMS:#{60000}}")
    private long rateDuration;

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitAspect(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Executed by each call of a method annotated with {@link RateLimit}.
     * This version uses Redis to store request counts per client IP.
     * If the request count exceeds the rate limit, a {@link RateLimitExceededException} is thrown.
     *
     * @throws RateLimitExceededException if rate limit for a given IP has been exceeded
     */
    @Before("@annotation(com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config.RateLimit)")
    public void rateLimit() {
        final ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        final String clientIp = requestAttributes.getRequest().getRemoteAddr();
        final String redisKey = "rate_limit:" + clientIp;

        Long currentRequestCount = redisTemplate.opsForValue().increment(redisKey); // Increment the request count for the current IP
        if (currentRequestCount == null) {
            // Redis operation failed or key is missing, so handle gracefully by initializing the request count
            currentRequestCount = 1L;
            redisTemplate.opsForValue().set(redisKey, String.valueOf(currentRequestCount));
            redisTemplate.expire(redisKey, rateDuration, TimeUnit.MILLISECONDS);
        }

        // Set the expiration time only when the key is created or if we initialized a new key
        if (currentRequestCount == 1) {
            redisTemplate.expire(redisKey, rateDuration, TimeUnit.MILLISECONDS);
        }

        // If request count exceeds the rate limit, throw an exception
        if (currentRequestCount > rateLimit) {
            throw new RateLimitExceededException(String.format(ERROR_MESSAGE, requestAttributes.getRequest().getRequestURI(), clientIp, rateDuration));
        }
    }

}
