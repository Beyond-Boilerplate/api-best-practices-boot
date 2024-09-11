package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import java.io.IOException;

import static org.mockito.Mockito.*;

class RequestTypeFilterTest {

    private RequestTypeFilter requestTypeFilter;

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
        requestTypeFilter = new RequestTypeFilter();

        // Mock the static MDC class
        mockedMDC = mockStatic(MDC.class);
    }

    @AfterEach
    void tearDown() {
        // Ensure the static mock is closed after each test
        mockedMDC.close();
    }

    @Test
    void testHttpMethodAndUrlAddedToMdcForGetRequest() throws ServletException, IOException {
        // Simulate a GET request to "/test"
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn("/test");

        // Run the filter
        requestTypeFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Verify that the HTTP method and URL are added to MDC
        mockedMDC.verify(() -> MDC.put("httpMethod", "GET"), times(1));
        mockedMDC.verify(() -> MDC.put("requestURL", "/test"), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("httpMethod"), times(1));
        mockedMDC.verify(() -> MDC.remove("requestURL"), times(1));
    }

    @Test
    void testHttpMethodAndUrlAddedToMdcForPostRequest() throws ServletException, IOException {
        // Simulate a POST request to "/submit"
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getRequestURI()).thenReturn("/submit");

        // Run the filter
        requestTypeFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Verify that the HTTP method and URL are added to MDC
        mockedMDC.verify(() -> MDC.put("httpMethod", "POST"), times(1));
        mockedMDC.verify(() -> MDC.put("requestURL", "/submit"), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("httpMethod"), times(1));
        mockedMDC.verify(() -> MDC.remove("requestURL"), times(1));
    }

    @Test
    void testMdcClearedEvenIfExceptionOccurs() throws ServletException, IOException {
        // Simulate a GET request to "/error"
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn("/error");

        // Simulate an exception during filter processing
        doThrow(new ServletException("Test Exception")).when(filterChain).doFilter(httpServletRequest, httpServletResponse);

        try {
            // Run the filter (this will throw an exception)
            requestTypeFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);
        } catch (ServletException e) {
            // Expected exception, do nothing
        }

        // Verify that the HTTP method and URL were added to MDC
        mockedMDC.verify(() -> MDC.put("httpMethod", "GET"), times(1));
        mockedMDC.verify(() -> MDC.put("requestURL", "/error"), times(1));

        // Verify that the MDC is still cleared even though an exception occurred
        mockedMDC.verify(() -> MDC.remove("httpMethod"), times(1));
        mockedMDC.verify(() -> MDC.remove("requestURL"), times(1));
    }

    @Test
    void testHttpMethodAndUrlAddedForSubPathRequest() throws ServletException, IOException {
        // Simulate a GET request to "/api/test/123"
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn("/api/test/123");

        // Run the filter
        requestTypeFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Verify that the HTTP method and URL are added to MDC
        mockedMDC.verify(() -> MDC.put("httpMethod", "GET"), times(1));
        mockedMDC.verify(() -> MDC.put("requestURL", "/api/test/123"), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("httpMethod"), times(1));
        mockedMDC.verify(() -> MDC.remove("requestURL"), times(1));
    }

    @Test
    void testHttpMethodAndUrlForRootPathRequest() throws ServletException, IOException {
        // Simulate a GET request to "/"
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn("/");

        // Run the filter
        requestTypeFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // Verify that the HTTP method and URL are added to MDC
        mockedMDC.verify(() -> MDC.put("httpMethod", "GET"), times(1));
        mockedMDC.verify(() -> MDC.put("requestURL", "/"), times(1));

        // Ensure the filter chain proceeds
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // Verify that the MDC is cleared after the request
        mockedMDC.verify(() -> MDC.remove("httpMethod"), times(1));
        mockedMDC.verify(() -> MDC.remove("requestURL"), times(1));
    }
}
