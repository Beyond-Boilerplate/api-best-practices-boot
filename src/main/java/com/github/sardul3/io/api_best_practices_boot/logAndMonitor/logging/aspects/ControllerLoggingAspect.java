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

/**
 * Aspect for logging input and output of all public methods in controllers.
 * <p>
 * This aspect is designed to automatically log incoming requests (input parameters)
 * and outgoing responses for any public method within the controller layer. It uses
 * Aspect-Oriented Programming (AOP) to minimize the need for repetitive logging code
 * within the controllers themselves.
 * </p>
 * <p>
 * The purpose of this implementation is to provide developers with a clear trace of
 * the flow of data between client requests and API responses, which is crucial for
 * debugging and monitoring.
 * </p>
 */
@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    private final ObjectMapper objectMapper;

    public ControllerLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Logs the input arguments and output responses for any public method in controllers.
     * <p>
     * This method intercepts calls to controller methods, logs the method being called,
     * the input parameters in JSON format, and the resulting output. If an exception occurs,
     * it logs the exception details.
     * </p>
     *
     * @param joinPoint the join point representing the intercepted method call
     * @return the result of the intercepted method
     * @throws Throwable if any error occurs during method execution
     */
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

    /**
     * Fetches the description from the custom annotation (@EndpointDescribe) if present,
     * otherwise falls back to using the method signature as the description.
     *
     * @param joinPoint the join point representing the intercepted method
     * @return a string describing the intercepted method
     */
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

    /**
     * Serializes the input arguments or result to JSON format for logging purposes.
     * <p>
     * This utility method converts objects to JSON format. In case of a serialization failure,
     * it logs the error and returns a fallback message.
     * </p>
     *
     * @param object the object to serialize
     * @return the serialized JSON string, or a fallback message if serialization fails
     */
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object to JSON: {}", e.getMessage());
            return "Unable to convert to JSON";
        }
    }
}
