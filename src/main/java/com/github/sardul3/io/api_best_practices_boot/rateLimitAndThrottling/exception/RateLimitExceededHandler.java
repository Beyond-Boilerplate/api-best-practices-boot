package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpHeaders;

/**
 * Global exception handler for handling rate limit exceptions.
 *
 * <p>This handler captures {@link RateLimitExceededException} exceptions thrown during
 * request processing and returns a structured error response with HTTP status 429 (Too Many Requests).
 * Additionally, a "Retry-After" header is included in the response, informing the client
 * when they can retry the request.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *     <li>Captures all {@link RateLimitExceededException} and provides a structured response.</li>
 *     <li>Returns a 429 status code with the "Retry-After" header.</li>
 *     <li>Logs the exception details with a unique error ID for traceability.</li>
 * </ul>
 */
@ControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitExceededHandler {

    /**
     * Handles {@link RateLimitExceededException} by generating a structured error response
     * and returning a "Retry-After" header, which informs the client how long they must wait
     * before retrying their request.
     *
     * @param rateLimitException the exception thrown when the rate limit is exceeded.
     * @param request the HTTP request that triggered the rate limit.
     * @return a {@link ResponseEntity} containing the error details and HTTP status 429.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorMessage> handleInvalidFieldsInValidJson(final RateLimitExceededException rateLimitException, final HttpServletRequest request) {
        final ApiErrorMessage apiErrorMessage = rateLimitException.toApiErrorMessage(request.getRequestURI());
        logIncomingCallException(rateLimitException, apiErrorMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(rateLimitException.getRetryAfterSeconds()));
        return new ResponseEntity<>(apiErrorMessage, headers, HttpStatus.TOO_MANY_REQUESTS);
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
