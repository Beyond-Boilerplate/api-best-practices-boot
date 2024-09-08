package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for adding descriptions to methods for logging purposes.
 * <p>
 * This annotation is used by the {@link ControllerLoggingAspect} to add human-readable
 * descriptions to log statements. It allows developers to provide a more meaningful context
 * to logs than simply logging the method signature.
 * </p>
 * <p>
 * This makes logs more expressive and easier to understand, especially when debugging complex workflows.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EndpointDescribe {
    /**
     * The description to be used in log statements.
     *
     * @return a string representing the description of the method
     */
    String value();
}
