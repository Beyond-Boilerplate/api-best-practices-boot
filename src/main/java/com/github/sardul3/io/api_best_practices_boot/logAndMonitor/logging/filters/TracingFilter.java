package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TracingFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        // Get the correlation ID from the request or generate a new one
        String correlationId = httpServletRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Add correlation ID to the response header so it can be traced in the client
        httpServletResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        // Add correlation ID to the MDC (Mapped Diagnostic Context) for logging
        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

        try {
            chain.doFilter(request, response);  // Continue the filter chain
        } finally {
            // Clear the MDC after the request has been processed
            MDC.remove(MDC_CORRELATION_ID_KEY);
        }
    }
}
