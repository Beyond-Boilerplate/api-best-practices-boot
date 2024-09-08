package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestTypeFilter extends OncePerRequestFilter {

    private static final String MDC_METHOD_KEY = "httpMethod";
    private static final String MDC_URL_KEY = "requestURL";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Add HTTP method and URL to MDC
            MDC.put(MDC_METHOD_KEY, request.getMethod());
            MDC.put(MDC_URL_KEY, request.getRequestURI());

            // Proceed with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC after the request
            MDC.remove(MDC_METHOD_KEY);
            MDC.remove(MDC_URL_KEY);
        }
    }
}