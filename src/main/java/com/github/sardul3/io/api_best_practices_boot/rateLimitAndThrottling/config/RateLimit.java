package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used on methods that need rate limiting.
 *
 * <p>This is a marker annotation that can be applied to any method, typically on HTTP endpoints,
 * to indicate that the method is subject to rate limiting.</p>
 *
 * <p>When a method is annotated with {@link RateLimit}, it will trigger the
 * {@link RateLimitAspect} aspect to check whether the number of requests made
 * from a given client (identified by IP address) exceeds the defined rate limit.</p>
 *
 * <p>Rate limiting is done based on a simple request count over a specified time duration.</p>
 *
 * <p>This is a very basic implementation of rate limiting and should be
 * considered a starting point for more robust implementations. It does not
 * support distributed rate limiting, and the IP-based identification may not be suitable
 * for clients behind proxies or NAT.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
}
