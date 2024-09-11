package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class TracingFilterTest {

    private TracingFilter tracingFilter;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private FilterChain filterChain;

    private MockedStatic<MDC> mockedMDC;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tracingFilter = new TracingFilter();

        // Mock the static MDC class
        mockedMDC = mockStatic(MDC.class);
    }

    @AfterEach
    void tearDown() {
        // Ensure the static mock is closed after each test
        mockedMDC.close();
    }

    @Test
    void testCorrelationIdExistsInRequest() throws IOException, ServletException {
        // Simulate an existing correlation ID header
        String existingCorrelationId = "existing-correlation-id";
        when(httpServletRequest.getHeader("X-Correlation-Id")).thenReturn(existingCorrelationId);

        // Run the filter
        tracingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Verify that the correlation ID from the request is set in the response header
        verify(httpServletResponse).setHeader("X-Correlation-Id", existingCorrelationId);

        // Verify that the correlation ID is placed in the MDC
        mockedMDC.verify(() -> MDC.put("correlationId", existingCorrelationId), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("correlationId"), times(1));
    }

    @Test
    void testNoCorrelationIdInRequest() throws IOException, ServletException {
        // Simulate no correlation ID in the request
        when(httpServletRequest.getHeader("X-Correlation-Id")).thenReturn(null);

        // Run the filter
        tracingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Capture the correlation ID that was generated and set in the response header
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).setHeader(eq("X-Correlation-Id"), captor.capture());
        String generatedCorrelationId = captor.getValue();

        // Ensure that a new correlation ID was generated
        assertNotNull(generatedCorrelationId);

        // Verify that the new correlation ID is placed in the MDC
        mockedMDC.verify(() -> MDC.put("correlationId", generatedCorrelationId), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("correlationId"), times(1));
    }

    @Test
    void testEmptyCorrelationIdInRequest() throws IOException, ServletException {
        // Simulate an empty correlation ID in the request
        when(httpServletRequest.getHeader("X-Correlation-Id")).thenReturn("");

        // Run the filter
        tracingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Capture the correlation ID that was generated and set in the response header
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).setHeader(eq("X-Correlation-Id"), captor.capture());
        String generatedCorrelationId = captor.getValue();

        // Ensure that a new correlation ID was generated
        assertNotNull(generatedCorrelationId);

        // Verify that the new correlation ID is placed in the MDC
        mockedMDC.verify(() -> MDC.put("correlationId", generatedCorrelationId), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("correlationId"), times(1));
    }

    @Test
    void testBlankCorrelationIdInRequest() throws IOException, ServletException {
        // Simulate a correlation ID that is blank or contains only spaces
        when(httpServletRequest.getHeader("X-Correlation-Id")).thenReturn("   ");

        // Run the filter
        tracingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Capture the correlation ID that was generated and set in the response header
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).setHeader(eq("X-Correlation-Id"), captor.capture());
        String generatedCorrelationId = captor.getValue();

        // Ensure that a new correlation ID was generated
        assertNotNull(generatedCorrelationId);

        // Verify that the new correlation ID is placed in the MDC
        mockedMDC.verify(() -> MDC.put("correlationId", generatedCorrelationId), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("correlationId"), times(1));
    }

    @Test
    void testMdcIsClearedAfterRequestProcessing() throws IOException, ServletException {
        String correlationId = UUID.randomUUID().toString();

        // Simulate an existing correlation ID header
        when(httpServletRequest.getHeader("X-Correlation-Id")).thenReturn(correlationId);

        // Run the filter
        tracingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Verify that the correlation ID is placed in the MDC
        mockedMDC.verify(() -> MDC.put("correlationId", correlationId), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("correlationId"), times(1));
    }

    @Test
    void testGenerateUniqueCorrelationIdOnMultipleRequests() throws IOException, ServletException {
        // First request with no correlation ID
        when(httpServletRequest.getHeader("X-Correlation-Id")).thenReturn(null);

        // Run the filter for the first request
        tracingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Capture the first correlation ID
        ArgumentCaptor<String> firstCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse).setHeader(eq("X-Correlation-Id"), firstCaptor.capture());
        String firstGeneratedCorrelationId = firstCaptor.getValue();

        // Second request with no correlation ID
        when(httpServletRequest.getHeader("X-Correlation-Id")).thenReturn(null);

        // Run the filter for the second request
        tracingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Capture the second correlation ID
        ArgumentCaptor<String> secondCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpServletResponse, times(2)).setHeader(eq("X-Correlation-Id"), secondCaptor.capture());
        String secondGeneratedCorrelationId = secondCaptor.getValue();

        // Ensure that two unique correlation IDs were generated for two separate requests
        assertNotNull(firstGeneratedCorrelationId);
        assertNotNull(secondGeneratedCorrelationId);
        assertNotEquals(firstGeneratedCorrelationId, secondGeneratedCorrelationId);

        // Verify that MDC was populated and cleared twice (once per request)
        mockedMDC.verify(() -> MDC.put("correlationId", firstGeneratedCorrelationId), times(1));
        mockedMDC.verify(() -> MDC.put("correlationId", secondGeneratedCorrelationId), times(1));
        mockedMDC.verify(() -> MDC.remove("correlationId"), times(2));
    }
}
