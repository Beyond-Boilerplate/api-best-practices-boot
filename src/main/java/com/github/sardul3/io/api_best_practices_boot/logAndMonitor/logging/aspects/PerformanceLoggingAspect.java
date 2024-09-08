package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging the performance (execution time) of public methods in controllers.
 * <p>
 * This aspect tracks how long each public method in the controller layer takes to execute.
 * The execution time is logged along with the correlation ID (if available) to help
 * developers and operators identify performance bottlenecks in the API.
 * </p>
 * <p>
 * The design uses Aspect-Oriented Programming (AOP) to minimize manual code for performance logging,
 * making it easier to apply performance monitoring across multiple methods.
 * </p>
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger performanceLogger = LoggerFactory.getLogger("performanceLogger");
    private static final String MDC_EXECUTION_TIME_KEY = "executionTime";

    /**
     * Logs the performance metrics (execution time) of public methods in controllers.
     * <p>
     * This method intercepts calls to controller methods, measures the execution time,
     * and logs it along with the correlation ID (if present in MDC). The execution time
     * is also stored temporarily in MDC for potential use in other logs.
     * </p>
     *
     * @param joinPoint the join point representing the intercepted method call
     * @return the result of the intercepted method
     * @throws Throwable if any error occurs during method execution
     */
    @Around("execution(public * com.github.sardul3.io.api_best_practices_boot..controllers..*(..))")
    public Object logPerformanceMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        String methodName = joinPoint.getSignature().toShortString();

        long startTime = System.currentTimeMillis();

        Object result;
        try {
            result = joinPoint.proceed();  // Proceed with the method execution
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Add execution time to MDC for further logging
            MDC.put(MDC_EXECUTION_TIME_KEY, duration + "ms");

            // Log performance metrics
            performanceLogger.info("[{}] Method {} executed in {} ms", correlationId, methodName, duration);

            // Clean up execution time from MDC after the request is processed
            MDC.remove(MDC_EXECUTION_TIME_KEY);
        }

        return result;
    }
}
