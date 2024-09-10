package com.github.sardul3.io.api_best_practices_boot.rateLimitAndThrottling.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiErrorMessage {
    private final UUID id = UUID.randomUUID();
    private final int status;
    private final String error;
    private final String message;
    private final LocalDateTime timestamp = LocalDateTime.now(Clock.systemUTC());
    private final String path;
}
