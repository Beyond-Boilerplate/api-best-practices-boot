package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for {@link RateLimitExceededException}.
 *
 * <p>This handler is responsible for catching {@link RateLimitExceededException}s thrown
 * during request processing and returning a structured error response with HTTP status 429
 * (Too Many Requests).</p>
 *
 * <p>The error response includes a unique error ID, status code, error message, timestamp,
 * and the request path that triggered the rate limit. This provides a standardized error
 * response format for API clients.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *     <li>Handles all rate limit exceptions and logs them with a unique error ID.</li>
 *     <li>Provides a clean JSON response with detailed information about the error.</li>
 * </ul>
 */
@ControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitExceededHandler {

    /**
     * Handles {@link RateLimitExceededException} by returning an {@link ApiErrorMessage}.
     *
     * <p>This method is triggered when the rate limit is exceeded, and it generates a structured
     * error response for the client, including details about the request that caused the error.</p>
     *
     * @param rateLimitException the exception thrown when rate limiting is exceeded.
     * @param request the current HTTP request.
     * @return a {@link ResponseEntity} containing the error message and HTTP status 429.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorMessage> handleInvalidFieldsInValidJson(final RateLimitExceededException rateLimitException, final HttpServletRequest request) {
        final ApiErrorMessage apiErrorMessage = rateLimitException.toApiErrorMessage(request.getRequestURI());
        logIncomingCallException(rateLimitException, apiErrorMessage);
        return new ResponseEntity<>(apiErrorMessage, HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Logs the exception details, including the unique error ID and message.
     *
     * @param rateLimitException the exception that was thrown.
     * @param apiErrorMessage the structured error message to be logged.
     */
    private static void logIncomingCallException(final RateLimitExceededException rateLimitException, final ApiErrorMessage apiErrorMessage) {
        log.error(String.format("%s: %s", apiErrorMessage.getId(), rateLimitException.getMessage()), rateLimitException.getLocalizedMessage());
    }
}
