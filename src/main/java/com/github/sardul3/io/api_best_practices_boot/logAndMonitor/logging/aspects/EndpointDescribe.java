package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Custom annotation to add descriptions to methods for logging
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EndpointDescribe {
    String value();  // The description to use in logs
}
