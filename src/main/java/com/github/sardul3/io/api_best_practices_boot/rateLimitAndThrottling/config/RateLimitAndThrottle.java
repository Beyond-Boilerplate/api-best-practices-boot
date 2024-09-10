package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimitAndThrottle {
    // Specify the rate limit value, if -1, the config value will be used
    int limit() default -1;

    // Specify the rate limit duration in milliseconds, if -1, the config value will be used
    long duration() default -1;

    int throttleAfter() default -1;
}
