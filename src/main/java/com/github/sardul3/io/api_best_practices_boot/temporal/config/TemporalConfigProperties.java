package com.github.sardul3.io.api_best_practices_boot.temporal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "temporal")
public class TemporalConfigProperties {

    private Map<String, WorkflowConfig> workflows;
    private Map<String, ActivityConfig> activities;

    @Setter
    @Getter
    public static class WorkflowConfig {
        private Map<String, WorkflowVersionConfig> versions;

    }

    @Setter
    @Getter
    public static class WorkflowVersionConfig {
        private Duration executionTimeout;
        private Duration runTimeout;
        private String taskQueue;

    }

    @Setter
    @Getter
    public static class ActivityConfig {
        private Map<String, ActivityVersionConfig> versions;

    }

    @Setter
    @Getter
    public static class ActivityVersionConfig {
        private Duration scheduleToCloseTimeout;
        private Duration startToCloseTimeout;
        private RetryConfig retry;

        public static class RetryConfig {
            private Duration initialInterval;
            private int maxAttempts;
            private double backoffCoefficient;

            public Duration getInitialInterval() {
                return initialInterval;
            }

            public void setInitialInterval(Duration initialInterval) {
                this.initialInterval = initialInterval;
            }

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public double getBackoffCoefficient() {
                return backoffCoefficient;
            }

            public void setBackoffCoefficient(double backoffCoefficient) {
                this.backoffCoefficient = backoffCoefficient;
            }
        }
    }
}

