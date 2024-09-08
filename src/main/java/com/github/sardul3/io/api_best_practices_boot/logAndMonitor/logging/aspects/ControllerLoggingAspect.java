package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.aspects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    private final ObjectMapper objectMapper;

    public ControllerLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("execution(public * com.github.sardul3.io.api_best_practices_boot..controllers..*(..))")
    public Object logPublicMethodsInControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        String description = getLoggableDescription(joinPoint);
        Object[] args = joinPoint.getArgs();

        // Convert input arguments to JSON
        String argsJson = serializeToJson(args);

        logger.info("Incoming request to {} with arguments: {}", description, argsJson);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception ex) {
            logger.error("Exception in  {}: {}", description, ex.getMessage());
            throw ex;
        }
        // Convert the result (response) to JSON
        String resultJson = serializeToJson(result);

        logger.info("Response from {}: {}", description, resultJson);
        return result;
    }

    // Fetch the description from the @LoggableOperation annotation
    private String getLoggableDescription(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        // Check if the method is annotated with @LoggableOperation
        if (method.isAnnotationPresent(EndpointDescribe.class)) {
            EndpointDescribe loggableOperation = method.getAnnotation(EndpointDescribe.class);
            return loggableOperation.value();  // Return the description
        }

        // Fallback to method signature if annotation is not present
        return methodSignature.toShortString();
    }

    // Utility method to convert objects to JSON format
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object to JSON: {}", e.getMessage());
            return "Unable to convert to JSON";
        }
    }
}
