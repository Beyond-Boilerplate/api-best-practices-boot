package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger performanceLogger = LoggerFactory.getLogger("performanceLogger");
    private static final String MDC_EXECUTION_TIME_KEY = "executionTime";

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
