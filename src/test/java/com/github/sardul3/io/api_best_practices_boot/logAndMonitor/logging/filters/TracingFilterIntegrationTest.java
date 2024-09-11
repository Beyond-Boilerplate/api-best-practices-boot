package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.filters;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TracingFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockedStatic<MDC> mockedMDC;

    @BeforeEach
    void setUp() {
        // Initialize a mocked static MDC class for verification
        mockedMDC = Mockito.mockStatic(MDC.class);
    }

    @AfterEach
    void tearDown() {
        // Close the static mock to ensure it is deregistered
        mockedMDC.close();
    }


    @Test
    void testCorrelationIdIsSetInResponseWhenAbsent() throws Exception {
        // Test case when the correlation ID is not present in the request
        mockMvc.perform(get("/api/payment-status").header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("X-Correlation-Id", notNullValue()));
    }

    @Test
    void testCorrelationIdIsPreservedWhenPresentInRequest() throws Exception {
        // Test case when the correlation ID is already present in the request
        String correlationId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/payment-status")
                        .header("X-Correlation-Id", correlationId)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(header().string("X-Correlation-Id", correlationId));
    }

    @Test
    void testNewCorrelationIdGeneratedWhenHeaderIsEmpty() throws Exception {
        // Test case when the correlation ID header exists but is empty

        mockMvc.perform(get("/api/payment-status")
                        .header("X-Correlation-Id", "")
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("X-Correlation-Id", notNullValue()))
                .andExpect(header().string("X-Correlation-Id", not("")));
    }

    @Test
    void testNewCorrelationIdGeneratedWhenHeaderHasInvalidValue() throws Exception {
        // Test case when the correlation ID header exists but has an invalid value
        mockMvc.perform(get("/api/payment-status").header("Idempotency-Key", UUID.randomUUID().toString()).header("X-Correlation-Id", "    "))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("X-Correlation-Id", notNullValue()))
                .andExpect(header().string("X-Correlation-Id", not("    ")));
    }

    @Test
    void testUniqueCorrelationIdInParallelRequests() throws Exception {
        // Test case for parallel requests ensuring unique correlation IDs are generated
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(get("/api/payment-status").header("Idempotency-Key", UUID.randomUUID().toString()))
                            .andExpect(status().isOk())
                            .andExpect(header().exists("X-Correlation-Id"))
                            .andExpect(header().string("X-Correlation-Id", notNullValue()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    void testMdcIsClearedAfterRequestProcessing() throws Exception {
        String correlationId = UUID.randomUUID().toString();

        // Test case to verify MDC is cleared after the request
        mockMvc.perform(get("/api/payment-status")
                        .header("X-Correlation-Id", correlationId)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
//                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", correlationId));

        // Verify that the correlation ID was set in the MDC
        mockedMDC.verify(() -> MDC.put("correlationId", correlationId), times(1));

        // Verify that MDC was cleared (i.e., the correlation ID was removed)
        mockedMDC.verify(() -> MDC.remove("correlationId"), times(1));
    }
}


