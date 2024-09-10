package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception.RateLimitExceededException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Method that is executed before any method annotated with {@link RateLimit}.
     *
     * <p>This method tracks the number of requests made by the client (identified by IP address)
     * and checks if it exceeds the allowed limit. If the request count exceeds the rate limit
     * within the defined duration, a {@link RateLimitExceededException} is thrown.</p>
     *
     * <p>Requests older than the configured duration (e.g., 60 seconds) are automatically
     * cleaned up and no longer counted.</p>
     *
     * @throws RateLimitExceededException if the rate limit is exceeded for the given client IP.
     */
    @Before("@annotation(com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config.RateLimit)")
    public void rateLimit() {
        final ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        final String key = requestAttributes.getRequest().getRemoteAddr();
        final long currentTime = System.currentTimeMillis();
        requestCounts.putIfAbsent(key, new ArrayList<>());
        requestCounts.get(key).add(currentTime);
        cleanUpRequestCounts(currentTime);
        if (requestCounts.get(key).size() > rateLimit) {
            throw new RateLimitExceededException(String.format(ERROR_MESSAGE, requestAttributes.getRequest().getRequestURI(), key, rateDuration));
        }
    }

    /**
     * Helper method to clean up old requests that are outside the rate limiting time window.
     *
     * <p>This method goes through all tracked requests and removes any that are older than
     * the configured {@link #rateDuration}.</p>
     *
     * @param currentTime the current time in milliseconds.
     */
    private void cleanUpRequestCounts(final long currentTime) {
        requestCounts.values().forEach(l -> {
            l.removeIf(t -> timeIsTooOld(currentTime, t));
        });
    }

    /**
     * Determines whether a request is too old to be considered for rate limiting.
     *
     * @param currentTime the current time in milliseconds.
     * @param timeToCheck the time of the request to be checked.
     * @return true if the request is older than the allowed rate duration, false otherwise.
     */
    private boolean timeIsTooOld(final long currentTime, final long timeToCheck) {
        return currentTime - timeToCheck > rateDuration;
    }
}
