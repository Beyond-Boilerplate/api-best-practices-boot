# Technical RFC: Comprehensive Logging and Monitoring for Spring Boot API

---

## Overview

Comprehensive logging and monitoring are critical components of any production-ready API. They help track requests, detect errors, monitor API performance, and ensure that the API behaves as expected. This RFC provides a detailed breakdown of logging and monitoring best practices, including naive solutions and more sophisticated, production-ready implementations using libraries. The document also explores how to expand logging to handle various identifiers like API keys or user IDs, and it includes decision matrices, examples, and a roadmap for improving robustness.

---

## Goals

- **Traceability**: Ensure every API request can be traced end-to-end.
- **Debugging**: Provide detailed logs for debugging and troubleshooting.
- **Performance Monitoring**: Track API performance with detailed metrics such as latency, throughput, and error rates.
- **Alerting**: Implement alert mechanisms to notify admins when issues or performance degradation occurs.

---

## Prerequisites

- A Spring Boot API.
- Access to a logging framework (e.g., Logback, SLF4J).
- Optional: Logging infrastructure such as Elasticsearch, Fluentd, Kibana (EFK stack), or Grafana Loki.

---

## Key Concepts in Logging and Monitoring

### 1. **Logging for Requests and Responses**
Every API call should be logged, including metadata such as:
- HTTP method (GET, POST, PUT, DELETE)
- Request URL
- Client IP address or API key
- Request body (sanitized to exclude sensitive data)
- Response status code and body
- Execution time of each request

**Naive Solution**:
```java
@Component
public class SimpleLoggingInterceptor extends HandlerInterceptorAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        logger.info("Request received: {} {} from IP {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        logger.info("Response sent: {} for {} {}", response.getStatus(), request.getMethod(), request.getRequestURI());
    }
}
```

### 2. **Structured Logging**
In a production environment, structured logs are essential to allow parsing by log aggregation tools such as Elasticsearch or Grafana Loki.

**Production-Ready Implementation (JSON Logging)**:
```xml
<configuration>
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
    </appender>
    <root level="INFO">
        <appender-ref ref="JSON_FILE" />
    </root>
</configuration>
```
Structured logging ensures logs can be easily parsed and analyzed by tools, enabling advanced queries and visualizations.

---

## Theory: How Logging Works

Logging in Spring Boot typically involves intercepting HTTP requests and responses using filters or interceptors. Each request can be logged before entering the business logic, and the response can be logged after processing. Additionally, exception handlers log errors globally. Monitoring tools (e.g., Prometheus) can collect performance metrics such as execution times, request count, and error rates.

---

## Expanding to API-Key or User-Level Logging

To enhance logging for API keys or user-level information, we can capture and log additional metadata such as the API key, user ID, or session ID. This allows detailed tracing across distributed systems.

### Example for Logging API Keys:
```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String apiKey = request.getHeader("X-API-KEY");
    logger.info("Request received: {} {} with API-Key {}", request.getMethod(), request.getRequestURI(), apiKey);
    return true;
}
```

---

## Decision Matrix: Simple vs Advanced Logging Systems

| **Aspect**                   | **Naive Logging**                             | **Advanced Logging**                                 |
|------------------------------|-----------------------------------------------|-----------------------------------------------------|
| **Traceability**              | Limited to basic request/response logging     | Full traceability using correlation IDs, API keys   |
| **Log Format**                | Plaintext logs                               | Structured logs (e.g., JSON for Logstash, ELK)       |
| **Scalability**               | Not optimized for large-scale applications    | Distributed logging with tools like ELK or Loki     |
| **Performance**               | Minimal logging overhead                     | Performance monitored with APM tools (e.g., New Relic)|
| **Error Handling**            | Logs only if exceptions are manually handled  | Global error logging using `@ControllerAdvice`       |
| **Integration**               | Minimal logging via console or file           | Integration with log aggregation and alerting tools |
| **Request Identification**    | Logs client IP                               | Logs API key, user ID, and correlation IDs           |
| **Retention and Rotation**    | Simple file-based log rotation                | Centralized log storage with log rotation and retention |

---

## Production-Ready Implementation for Logging and Monitoring

### 1. **Global Exception Handling**
To log all exceptions globally, use `@ControllerAdvice`. This ensures that unhandled exceptions are captured and logged uniformly.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e, HttpServletRequest request) {
        logger.error("Error during request processing: {} {}", request.getMethod(), request.getRequestURI(), e);
        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### 2. **Correlation IDs**
Add correlation IDs to logs to trace individual requests across microservices or distributed systems. Use a `Filter` to generate or retrieve a correlation ID and log it with each request.

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
        }
        MDC.put("correlationId", correlationId);
        filterChain.doFilter(request, response);
        MDC.remove("correlationId");
    }
}
```

### 3. **Health Checks**
Spring Boot’s Actuator allows you to expose health checks at `/actuator/health`, which can be monitored via tools like Prometheus or AWS CloudWatch.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics
  endpoint:
    health:
      show-details: always
```

### 4. **Performance Monitoring**
Use Micrometer for performance monitoring by instrumenting your code with timers and counters.

```java
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MetricsService {
    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void trackRequest(String endpoint) {
        meterRegistry.counter("api.requests", "endpoint", endpoint).increment();
    }
}
```

---

## Comparison: Naive vs Production-Ready Systems

### Simple Logging System (Naive)
- Logs basic request/response information.
- File-based logging with no centralized log management.
- Lacks structured logging (hard to parse and analyze).
- Minimal monitoring; does not collect performance metrics.

### Advanced Logging System (Production-Ready)
- Structured logging (JSON) with centralized logging tools (e.g., ELK, Loki).
- Logs detailed request/response information, including API keys, user IDs, and correlation IDs.
- Monitors performance metrics such as request latency and error rates with tools like Prometheus and Grafana.
- Logs are automatically rotated and archived, with alerts on error spikes.

---

## 15 Improvements to Make the Solution Production-Ready

1. **Dynamic Log Levels**: Implement endpoints to dynamically adjust log levels without restarting the application.
2. **Sanitize Logs**: Automatically redact sensitive data (e.g., passwords, PII) from logs.
3. **Correlation Across Microservices**: Use a shared correlation ID for requests that traverse multiple services.
4. **Distributed Tracing**: Integrate with tracing libraries (e.g., OpenTelemetry) to trace requests across services.
5. **Log Sampling**: Implement log sampling for high-traffic services to avoid overwhelming the logging infrastructure.
6. **Custom Log Fields**: Add additional context (e.g., user ID, role, API key) to each log entry.
7. **Log Aggregation**: Centralize logs using a log aggregation platform (e.g., ELK stack or Grafana Loki).
8. **Error Alerting**: Set up alerts for specific error patterns using tools like Prometheus AlertManager or PagerDuty.
9. **Log Retention Policy**: Implement a policy for log retention (e.g., retain logs for 30 days and archive or delete older logs).
10. **Track Custom Business Metrics**: Capture and log business-specific metrics like payment failures or user registrations.
11. **API Request Limits**: Monitor and log request counts per API key or user to prevent abuse.
12. **Health Monitoring**: Expose detailed health metrics for all external dependencies (e.g., databases, message queues).
13. **Custom Error Pages**: Log errors and

display friendly error pages to users without revealing sensitive information.
14. **Rate Limiting Logs**: Capture logs for rate-limiting and throttling events to analyze API usage patterns.
15. **Asynchronous Logging**: Use asynchronous logging to avoid blocking the main request thread during high-volume log writes.

---

## Checklist for Implementing Logging

### **Starter**
1. Log incoming requests (method, URI, client IP).
2. Log outgoing responses (status code, URI).
3. Handle global exceptions with `@ControllerAdvice`.
4. Rotate logs based on time or file size.

### **Intermediate**
1. Add structured logging (e.g., JSON logs for ELK).
2. Implement correlation IDs for tracing.
3. Track custom metrics (e.g., request latency, API usage).
4. Centralize logs using a log aggregation tool.
5. Implement health checks with Spring Boot Actuator.

### **Advanced**
1. Set up dynamic log level control via API endpoints.
2. Integrate with APM tools (e.g., New Relic, Datadog).
3. Sanitize logs to remove sensitive data.
4. Set up alerting on error spikes or performance degradation.
5. Implement distributed tracing with OpenTelemetry or similar tools.
6. Enable log sampling and retention policies to manage log volume.
7. Capture and analyze business metrics (e.g., failed transactions, high-priority feature usage).
8. Use asynchronous logging to improve performance under high load.

---

By following the steps outlined in this RFC, you can transform a simple logging solution into a production-ready logging and monitoring system that scales well, supports distributed environments, and provides deep insights into API usage and performance.

# Technical RFC: Comprehensive Monitoring for Spring Boot API Using Prometheus and Grafana

---

## Overview

Effective monitoring is crucial for ensuring the reliability, performance, and scalability of production APIs. Monitoring provides insights into the API’s health and usage patterns, enabling administrators to detect and address potential issues before they impact end users. This RFC outlines detailed steps to set up monitoring for a Spring Boot API, including metrics collection, alerts, and dashboard creation. It focuses on using **Prometheus** for metrics collection and **Grafana** for visualization, with a detailed explanation of setting up dashboards for monitoring various API metrics.

---

## Goals

- **Visibility**: Ensure complete visibility into API performance, request patterns, and potential bottlenecks.
- **Alerting**: Automatically notify administrators when issues occur or thresholds are crossed.
- **Optimization**: Use metrics to optimize API performance and reliability.
- **Dashboards**: Provide rich visualizations of metrics in Grafana, allowing easy interpretation of data.

---

## Prerequisites

- **Spring Boot** application.
- **Prometheus** for collecting metrics.
- **Grafana** for visualizing metrics.
- **Micrometer** for exposing metrics to Prometheus from Spring Boot.

---

## Key Metrics for a Production-Ready API

A production-ready API should track key performance indicators (KPIs) and operational metrics. These metrics are grouped into categories such as request metrics, performance, error tracking, and system health.

### 1. **Request Metrics**
- **Total requests per endpoint**: Count of all requests received per endpoint.
- **Requests per minute (RPM)**: The number of requests handled by the API per minute.
- **Requests by HTTP method**: Breakdown of requests by GET, POST, PUT, DELETE.
- **Success vs. Error Requests**: Track the percentage of successful vs. failed requests.
- **Status Codes**: Track responses based on their status codes (200, 400, 500).

### 2. **Performance Metrics**
- **Latency/Response Time per Endpoint**: Average time taken by the API to process requests per endpoint.
- **99th Percentile Latency**: Track the 99th percentile of response time to identify outliers.
- **Throughput per Endpoint**: Number of requests served successfully by each endpoint.

### 3. **Error Tracking**
- **Error Rate per Endpoint**: Percentage of requests resulting in errors (4xx or 5xx).
- **Exceptions per Endpoint**: Track unhandled exceptions and server errors (500 status codes).
- **Slow Requests**: Identify requests that take longer than a specific threshold (e.g., > 2s).

### 4. **System Health Metrics**
- **Memory Usage**: JVM memory usage, including heap and non-heap memory.
- **CPU Usage**: Percentage of CPU used by the API server.
- **Active Threads**: Number of active threads in the application.
- **Database Connections**: Number of open database connections.

---

## Enabling Monitoring with Prometheus and Micrometer

### Step 1: Add Micrometer Dependencies

To enable monitoring, first add the necessary Micrometer and Prometheus dependencies to your **Spring Boot** project.

**Gradle**:
```groovy
implementation 'io.micrometer:micrometer-core'
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

**Maven**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Step 2: Configure Spring Boot Actuator

Expose Prometheus-compatible metrics via the `/actuator/prometheus` endpoint by enabling it in `application.yml`.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, metrics
  metrics:
    tags:
      application: your-app-name
```

This setup will expose metrics automatically on `/actuator/prometheus`, which Prometheus will scrape.

### Step 3: Set Up Prometheus

Install and configure **Prometheus** to scrape metrics from the Spring Boot API.

1. Download and install Prometheus: https://prometheus.io/download/
2. Modify the `prometheus.yml` configuration file to scrape the metrics from your Spring Boot application.

```yaml
scrape_configs:
  - job_name: 'spring-boot-api'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']  # Change the target if your app runs on a different port
```

### Step 4: Set Up Grafana for Visualization

Install **Grafana** and configure it to fetch metrics from Prometheus.

1. Download and install Grafana: https://grafana.com/get
2. Add Prometheus as a data source in Grafana:
    - Go to **Configuration** -> **Data Sources**.
    - Select **Prometheus**.
    - Set the URL to `http://localhost:9090` (or wherever Prometheus is running).
    - Save the data source.

---

## Detailed Steps for Setting Up Meaningful Dashboards in Grafana

### Dashboard Creation Overview

A well-configured Grafana dashboard can give a complete picture of your API’s health and performance. The key metrics outlined above should be displayed in easy-to-read panels with appropriate visualizations (e.g., bar charts, line graphs, gauges).

Here are **20 meaningful dashboards** for monitoring a Spring Boot API:

### 1. **Total Requests per Endpoint**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Bar chart
- **Description**: Displays the number of requests received per endpoint.

**Query**:
```promql
sum by (uri) (http_server_requests_seconds_count)
```

### 2. **Requests per Minute (RPM)**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Line graph
- **Description**: Displays the rate of requests per minute for each endpoint.

**Query**:
```promql
rate(http_server_requests_seconds_count[1m])
```

### 3. **Requests by HTTP Method**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Pie chart
- **Description**: Breakdown of requests by HTTP method (GET, POST, PUT, DELETE).

**Query**:
```promql
sum by (method) (http_server_requests_seconds_count)
```

### 4. **Success vs Error Requests**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Gauge
- **Description**: Tracks the percentage of successful (2xx) vs failed (4xx, 5xx) requests.

**Query**:
```promql
(sum(http_server_requests_seconds_count{status=~"2.."})) / sum(http_server_requests_seconds_count) * 100
```

### 5. **Status Codes Breakdown**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Bar chart
- **Description**: Shows how many responses fall into each status code category (2xx, 4xx, 5xx).

**Query**:
```promql
sum by (status) (http_server_requests_seconds_count)
```

### 6. **Latency per Endpoint**
- **Metric**: `http_server_requests_seconds_sum / http_server_requests_seconds_count`
- **Panel Type**: Heatmap or line graph
- **Description**: Average response time per endpoint.

**Query**:
```promql
sum by (uri) (http_server_requests_seconds_sum) / sum by (uri) (http_server_requests_seconds_count)
```

### 7. **99th Percentile Latency**
- **Metric**: `http_server_requests_seconds`
- **Panel Type**: Gauge
- **Description**: Track the 99th percentile of response times for each endpoint.

**Query**:
```promql
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, uri))
```

### 8. **Throughput per Endpoint**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Bar chart
- **Description**: Number of requests successfully handled per endpoint.

**Query**:
```promql
rate(http_server_requests_seconds_count{status=~"2.."}[1m])
```

### 9. **Error Rate per Endpoint**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Line graph
- **Description**: Percentage of requests that return 4xx or 5xx status codes per endpoint.

**Query**:
```promql
(sum(http_server_requests_seconds_count{status=~"4..|5.."}) by (uri)) / sum(http_server_requests_seconds_count) by (uri) * 100
```

### 10. **Exceptions per Endpoint**
- **Metric**: `http_server_requests_seconds_count`
- **Panel Type**: Table
- **Description**: Number of exceptions (5xx status) encountered per endpoint.

**Query**:
```promql
sum(http_server_requests_seconds_count{status=~"5.."}) by (uri)
```

### 11. **Slow Requests**
- **Metric**: `http_server_requests_seconds_bucket`
- **Panel Type**: Table
- **Description**: Identify requests that take longer than 2 seconds.

**Query**:
```promql
sum(http_server_requests_seconds_bucket{le="2"} - http_server_requests_seconds_bucket

{le="1"}) by (uri)
```

### 12. **Memory Usage**
- **Metric**: `jvm_memory_used_bytes`
- **Panel Type**: Gauge
- **Description**: Monitor JVM heap and non-heap memory usage.

**Query**:
```promql
sum(jvm_memory_used_bytes) by (area)
```

### 13. **CPU Usage**
- **Metric**: `process_cpu_seconds_total`
- **Panel Type**: Line graph
- **Description**: CPU usage of the Spring Boot application.

**Query**:
```promql
rate(process_cpu_seconds_total[1m])
```

### 14. **Active Threads**
- **Metric**: `jvm_threads_live_threads`
- **Panel Type**: Gauge
- **Description**: Tracks the number of live threads.

**Query**:
```promql
jvm_threads_live_threads
```

### 15. **Database Connections**
- **Metric**: `hikaricp_connections_active`
- **Panel Type**: Line graph
- **Description**: Monitor active database connections.

**Query**:
```promql
hikaricp_connections_active
```

### 16. **GC Time**
- **Metric**: `jvm_gc_pause_seconds_sum`
- **Panel Type**: Line graph
- **Description**: Monitor garbage collection pause time.

**Query**:
```promql
rate(jvm_gc_pause_seconds_sum[1m])
```

### 17. **Heap Usage**
- **Metric**: `jvm_memory_used_bytes`
- **Panel Type**: Line graph
- **Description**: Monitor heap memory usage.

**Query**:
```promql
jvm_memory_used_bytes{area="heap"}
```

### 18. **Non-Heap Usage**
- **Metric**: `jvm_memory_used_bytes`
- **Panel Type**: Line graph
- **Description**: Monitor non-heap memory usage.

**Query**:
```promql
jvm_memory_used_bytes{area="nonheap"}
```

### 19. **Thread Pool Usage**
- **Metric**: `jvm_threads_live_threads`
- **Panel Type**: Line graph
- **Description**: Monitor the number of active threads in the application.

**Query**:
```promql
jvm_threads_live_threads
```

### 20. **Response Size**
- **Metric**: `http_server_requests_seconds_sum`
- **Panel Type**: Line graph
- **Description**: Track the response size of requests over time.

**Query**:
```promql
sum(rate(http_server_requests_seconds_sum[1m])) by (uri)
```

---

## Step-by-Step Guide for Setting Up Grafana Dashboards

1. **Add Prometheus as a Data Source**:
    - In Grafana, go to **Configuration** -> **Data Sources**.
    - Add **Prometheus** as the data source and point it to `http://localhost:9090`.

2. **Create a New Dashboard**:
    - Click **Create** -> **Dashboard** -> **Add Query**.
    - Choose **Prometheus** as the data source and enter the query.

3. **Add Multiple Panels**:
    - Each metric can have its own panel. For example, one panel for latency, another for error rates, and so on.
    - Use visualizations such as line graphs, bar charts, or tables based on the metric.

4. **Customize Panels**:
    - Set appropriate titles, descriptions, and units (e.g., milliseconds for latency, percentages for error rates).
    - Adjust thresholds and color schemes to highlight critical metrics (e.g., red for high latency or error rates).

5. **Save and Share Dashboards**:
    - Save the dashboard and optionally share it with your team by providing a URL or exporting it as JSON.

---

## Alerts and Notifications

Set up alerts in Grafana to trigger notifications when certain thresholds are breached (e.g., high latency or high error rates).

1. **Create Alerts**:
    - In the panel, click **Edit** -> **Alert**.
    - Set conditions for when the alert should trigger (e.g., latency > 500ms for 5 minutes).
    - Configure notifications (e.g., email, Slack) for the alert.

2. **Alert Rules**:
    - Alerts can be based on real-time metrics (e.g., request count, response time).
    - Set up escalation policies if alerts are not acknowledged within a certain timeframe.

---

## Summary

By following these steps, you can set up a robust monitoring solution for your Spring Boot API, providing rich insights into performance, reliability, and usage. With Prometheus collecting metrics and Grafana visualizing them, your API’s health and behavior can be monitored effectively, allowing you to optimize performance and ensure reliability.

