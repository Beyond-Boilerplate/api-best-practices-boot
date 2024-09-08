package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to log the client's IP address for every incoming request.
 * <p>
 * This filter adds the client's IP address to the MDC (Mapped Diagnostic Context),
 * allowing it to be included in all logs during the lifecycle of the request. The IP
 * address is also logged directly at the beginning of each request.
 * </p>
 */
@Component
public class IPAddressLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IPAddressLoggingFilter.class);
    // Key for storing the IP address in MDC
    private static final String MDC_IP_KEY = "clientIpAddress";

    /**
     * Filters the request to log the client's IP address.
     * <p>
     * The IP address of the incoming request is added to the MDC so that it can be included
     * in all log statements during the lifecycle of the request. Once the request is processed,
     * the IP is removed from the MDC.
     * </p>
     *
     * @param request  the incoming HTTP request
     * @param response the outgoing HTTP response
     * @param filterChain the filter chain
     * @throws ServletException, IOException if an exception occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // Get the client IP address
            String clientIpAddress = request.getRemoteAddr();
            // Put the IP address into MDC
            MDC.put(MDC_IP_KEY, clientIpAddress);

            // Proceed with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Remove the IP address from MDC after the request has been processed
            MDC.remove(MDC_IP_KEY);
        }
    }
}

