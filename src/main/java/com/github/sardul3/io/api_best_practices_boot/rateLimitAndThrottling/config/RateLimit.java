package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used on methods that require rate limiting.
 *
 * <p>This annotation can be applied to any method, typically on HTTP endpoints,
 * to indicate that the method is subject to rate limiting based on the number of requests
 * made from a client, identified by their IP address.</p>
 *
 * <p>The rate limit configuration can either be specified within the annotation (limit and duration),
 * or be sourced from external configuration files such as application.yml.</p>
 *
 * <p>The annotation provides two optional parameters:</p>
 * <ul>
 *   <li>{@code limit}: Specifies the number of allowed requests within the time window.
 *   If set to -1, the value will be retrieved from the application configuration.</li>
 *   <li>{@code duration}: Specifies the time window (in milliseconds) for counting requests.
 *   If set to -1, the value will be retrieved from the application configuration.</li>
 * </ul>
 *
 * <p>This implementation is based on a simple request count over a time window and uses
 * Redis to store the rate-limiting state. IP addresses are used to track request counts per client.
 * More sophisticated techniques may be needed for production environments, such as using distributed
 * rate limiting strategies.</p>
 *
 * @see RateLimitAspect
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
    // Specify the rate limit value, if -1, the config value will be used
    int limit() default -1;

    // Specify the rate limit duration in milliseconds, if -1, the config value will be used
    long duration() default -1;
}
