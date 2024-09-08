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

@Component
public class IPAddressLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IPAddressLoggingFilter.class);
    // Key for storing the IP address in MDC
    private static final String MDC_IP_KEY = "clientIpAddress";


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

