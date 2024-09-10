package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a client exceeds the allowed number of requests within the rate limit window.
 *
 * <p>This exception triggers a response with HTTP status 429 (Too Many Requests) and includes
 * a custom error message with details about the rate limit and when the client can try again.</p>
 *
 * <p>For simplicity, this basic implementation provides the error message and a timestamp,
 * but it can be extended to include more details like retry-after headers, request-specific data, etc.</p>
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds; // The time to retry after (in seconds)

    public RateLimitExceededException(final String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Converts the exception to an {@link ApiErrorMessage} for a structured response format.
     *
     * @param path the request path that triggered the rate limit.
     * @return an {@link ApiErrorMessage} containing the error details.
     */
    public ApiErrorMessage toApiErrorMessage(final String path) {
        return new ApiErrorMessage(HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.name(), this.getMessage(), path);
    }
}