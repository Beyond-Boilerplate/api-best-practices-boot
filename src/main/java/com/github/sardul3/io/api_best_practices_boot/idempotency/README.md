# Idempotency in API Design

## What is Idempotency?

Idempotency is the property of certain operations in which performing the same operation multiple times results in the same outcome. In the context of API design, **idempotency** ensures that repeated requests with the same parameters produce the same result, without unintended side effects such as duplicate charges, repeated record creation, or unnecessary operations.

For example, if a payment request is sent multiple times due to a network retry, the backend should handle the request idempotently and ensure the payment is only processed once.

## Why Idempotency Matters?

Idempotency is critical in scenarios where network instability or client retries may occur. Without idempotency, a client might inadvertently submit multiple requests, leading to undesirable results like duplicate payments, multiple user account creations, or erroneous data updates.

### Key Benefits:
- **Reliability**: Guarantees that repeated operations don't cause unwanted changes or duplicate actions.
- **User Experience**: Ensures users don't face double charges or unexpected results when resubmitting forms or retrying failed operations.
- **Data Integrity**: Helps in maintaining consistent and clean data, preventing multiple writes for the same operation.

## Real-Life Use Case: Idempotency in Payment Processing

Consider an e-commerce scenario where a customer submits a payment request. Due to network instability, the request might be sent multiple times. Without idempotency, the server may process multiple payments for the same order, resulting in the customer being charged multiple times.

### Example Scenarios Where Idempotency is Critical:
- **Payment Gateways**: Preventing duplicate transactions when a payment is retried due to failures or timeouts.
- **API Resource Creation**: Ensuring that creating resources (e.g., user accounts) does not lead to duplicates.
- **Message Processing**: In systems like Kafka or RabbitMQ, idempotency ensures that the same message is not processed more than once.

## Idempotent and Non-Idempotent Methods

- **Idempotent Methods**: Methods such as `GET`, `PUT`, and `DELETE` are typically idempotent. Performing the same `GET` request multiple times will return the same result. Similarly, sending a `DELETE` request multiple times for the same resource should result in the resource being deleted (or no resource found after the first deletion).

- **Non-Idempotent Methods**: Methods like `POST` are generally not idempotent. If you submit a form multiple times, it might create multiple resources. However, you can implement idempotency for `POST` requests by using techniques like unique identifiers or idempotency keys.

## Techniques for Ensuring Idempotency

1. **Idempotency Keys**:
    - One of the most common methods to ensure idempotency is to include an **idempotency key** in requests. This key uniquely identifies the request and is stored on the server. If the same key is received again, the server can return the cached result instead of processing the request again.

2. **Token-Based Idempotency**:
    - Some systems use unique tokens, session identifiers, or client-generated UUIDs to ensure that multiple identical requests produce the same result.

3. **Conditional Writes**:
    - APIs can include conditional headers like `If-Match` or `If-None-Match` to determine if the resource was updated since the last request.

## Real-World Example: Idempotency in Payment Processing

### Current Implementation:
In this repository, the **PaymentController** ensures idempotency using Redis and idempotency keys. When a payment request is submitted:
1. **Idempotency Key Handling**: The client sends an idempotency key with the request.
2. **State Machine for Payment Status**: We implemented a state machine for `PENDING`, `PROCESSING`, and `COMPLETED` states.
3. **Redis Caching**: The state and transaction results are stored in Redis for quick lookups, preventing duplicate processing.

### Decision Matrix on Why This Approach Works:
| **Factor**                 | **Current Approach**                                  | **Why It’s the Best**                                                        |
|----------------------------|------------------------------------------------------|-------------------------------------------------------------------------------|
| Handling Duplicate Requests | Idempotency keys stored in Redis                     | Prevents duplicate payments from being processed.                             |
| Quick State Lookup          | Redis caching of payment state (`PENDING`, `COMPLETED`) | Low-latency state checks ensure fast responses to repeated requests.           |
| Robustness                  | Locking mechanism to prevent race conditions         | Ensures only one process handles a payment at a time, improving system stability.|
| Scalability                 | Statelessness with Redis-backed storage              | Supports high traffic and scales horizontally across distributed systems.      |

### Example Flow:
1. **First Request**: A payment is submitted, and the server processes it, marking the status as `COMPLETED` in Redis.
2. **Subsequent Requests with the Same Key**: Instead of reprocessing the payment, the server returns the cached `COMPLETED` response.

Best Practices for Implementing Idempotency
-------------------------------------------

1.  **Generate Idempotency Keys at the Client**: Ensure that the client generates a unique idempotency key for each request.

2.  **Set Expiration Time for Idempotency Keys**: To avoid stale data, use expiration times for keys stored in Redis.

3.  **Use Locks to Avoid Race Conditions**: Ensure that only one process is handling a given idempotency key at a time by implementing locking mechanisms.

4.  **Return Consistent Results**: Always return the same result for the same idempotency key, even if the request is retried multiple times.

5.  **Monitor for Idempotency Failures**: Implement logging and monitoring to track idempotency failures or exceptions for debugging.

API Endpoints
-------------

### 1\. Generate Idempotency Key

*   **Endpoint**: /api/server-generated/generate-key

*   **Method**: GET

*   **Description**: Generates a unique idempotency key for the client to use in subsequent payment requests.


### 2\. Process Payment

*   **Endpoint**: /api/payment

*   **Method**: POST

*   **Headers**:

    *   Idempotency-Key: The key generated from the /generate-key endpoint.

*   **Body**: JSON object containing payment details (e.g., amount, currency).

*   **Description**: Processes a payment using the provided idempotency key and payment details.


### 3\. Get Payment Status

*   **Endpoint**: /api/payment-status

*   **Method**: GET

*   **Headers**:

    *   Idempotency-Key: The idempotency key used during payment processing.

*   **Description**: Retrieves the status of a payment associated with the given idempotency key.


Flow Description
----------------

### 1\. Generating an Idempotency Key

*   **Client Action**: Sends a GET request to /api/server-generated/generate-key.

*   **Server Action**:

    *   Generates a UUID idempotency key.

    *   Stores the key in Redis with an initial value "KEY GENERATED" and an expiration time.

    *   Returns the idempotency key to the client.


### 2\. Processing a Payment

*   **Client Action**: Sends a POST request to /api/payment with the idempotency key and payment details.

*   **Server Action**:

    *   Validates the presence of the idempotency key.

    *   Acquires a lock for the idempotency key to prevent concurrent processing.

    *   Checks if the key exists in Redis:

        *   If the key doesn't exist, throws an InvalidIdempotencyKeyException.

        *   If the key's value is not "KEY GENERATED", returns the cached transaction result.

    *   Processes the payment (simulated).

    *   Updates the key's value in Redis with the transaction ID.

    *   Releases the lock.

    *   Returns a success response with the transaction ID.


### 3\. Retrieving Payment Status

*   **Client Action**: Sends a GET request to /api/payment-status with the idempotency key.

*   **Server Action**:

    *   Retrieves the payment status from Redis using the idempotency key.

    *   If the key doesn't exist or payment is not completed, throws a PaymentNotFoundException.

    *   Returns the payment status.


Happy Paths
-----------

### Scenario A: Successful Payment Processing

1.  
```
curl -X GET http://localhost:8080/api/server-generated/generate-key**Response**:jsonCopy code"9337393d-b8f5-4ec6-9b61-f9a24333daf3"

```

2.  
```angular2html
curl -X POST http://localhost:8080/api/payment \\ -H "Idempotency-Key: 9337393d-b8f5-4ec6-9b61-f9a24333daf3" \\ -H "Content-Type: application/json" \\ -d '{"amount": "100", "currency": "USD"}'**Response**:jsonCopy code"Payment processed successfully with Transaction ID: TXN1693856405000"
```

3.
```
curl -X GET http://localhost:8080/api/payment-status \\ -H "Idempotency-Key: 9337393d-b8f5-4ec6-9b61-f9a24333daf3"**Response**:jsonCopy code"Transaction ID: TXN1693856405000"
```


### Scenario B: Duplicate Payment Request with Same Idempotency Key
```
curl -X POST http://localhost:8080/api/payment \\ -H "Idempotency-Key: 9337393d-b8f5-4ec6-9b61-f9a24333daf3" \\ -H "Content-Type: application/json" \\ -d '{"amount": "100", "currency": "USD"}'**Response**:jsonCopy code"Transaction ID: TXN1693856405000"

```
    *   **Explanation**: The server returns the cached transaction result without reprocessing the payment.


Unhappy Paths (Error Scenarios)
-------------------------------

### Error 1: Missing Idempotency Key
```
curl -X POST http://localhost:8080/api/payment \\ -H "Content-Type: application/json" \\ -d '{"amount": "100", "currency": "USD"}'

```

*   **Exception Thrown**: MissingIdempotencyKeyException

*   **HTTP Status**: 400 Bad Request

```
{ "status": 400, "error": "Bad Request", "message": "Missing Idempotency-Key header."}
```

### Error 2: Invalid or Non-Existent Idempotency Key

```
curl -X POST http://localhost:8080/api/payment \\ -H "Idempotency-Key: invalid-key" \\ -H "Content-Type: application/json" \\ -d '{"amount": "100", "currency": "USD"}'
```
*   **Exception Thrown**: InvalidIdempotencyKeyException

*   **HTTP Status**: 400 Bad Request

```
{ "status": 400, "error": "Bad Request", "message": "Invalid or missing Idempotency-Key."}

```


### Error 3: Payment Already in Progress

*   **Action**: Two concurrent requests are made with the same idempotency key.

``` 
curl -X POST http://localhost:8080/api/payment \\ -H "Idempotency-Key: 9337393d-b8f5-4ec6-9b61-f9a24333daf3" \\ -H "Content-Type: application/json" \\ -d '{"amount": "100", "currency": "USD"}'

```
```
curl -X POST http://localhost:8080/api/payment \\ -H "Idempotency-Key: 9337393d-b8f5-4ec6-9b61-f9a24333daf3" \\ -H "Content-Type: application/json" \\ -d '{"amount": "100", "currency": "USD"}'
```
*   **Exception Thrown**: PaymentInProgressException (for the second request)

*   **HTTP Status**: 409 Conflict

```
{ "status": 409, "error": "Conflict", "message": "Payment is already being processed."}
```

### Error 4: No Payment Found for Idempotency Key

```
 curl -X GET http://localhost:8080/api/payment-status \\ -H "Idempotency-Key: invalid-key"
```
*   **Exception Thrown**: PaymentNotFoundException

*   **HTTP Status**: 404 Not Found

```
{ "status": 404, "error": "Not Found", "message": "No payment found for this Idempotency-Key."}
```

Exceptions Thrown
-----------------

*   **MissingIdempotencyKeyException**

    *   **When Thrown**: The Idempotency-Key header is missing or empty in the request.

    *   **HTTP Status**: 400 Bad Request

*   **InvalidIdempotencyKeyException**

    *   **When Thrown**: The provided idempotency key does not exist in Redis or was not generated by the server.

    *   **HTTP Status**: 400 Bad Request

*   **PaymentInProgressException**

    *   **When Thrown**: A lock on the idempotency key is already acquired, indicating that the payment is currently being processed.

    *   **HTTP Status**: 409 Conflict

*   **PaymentNotFoundException**

    *   **When Thrown**: No payment associated with the idempotency key is found, or the payment is not yet completed.

    *   **HTTP Status**: 404 Not Found


Conclusion
----------

By implementing idempotency using server-generated keys, Redis caching, and proper exception handling, we ensure that:

*   **Duplicate Requests**: Are handled gracefully by returning cached results, preventing duplicate payments.

*   **Error Scenarios**: Provide clear and descriptive responses, allowing clients to understand and rectify issues.

*   **Concurrency**: Race conditions are avoided using a locking mechanism, ensuring only one process handles a payment at a time.


This approach enhances the reliability and robustness of the payment processing API, providing a better user experience and maintaining data integrity.


Conclusion
----------

Idempotency is essential for building reliable and fault-tolerant APIs, especially in systems where network issues, retries, or user errors may occur. By leveraging techniques like idempotency keys and state management, you can ensure that repeated requests do not result in unintended side effects or duplicate actions. The approach implemented in this repository, combining Redis with a state machine for payment processing, is an efficient and scalable solution for ensuring idempotency.

### Final Segment for README: How to Run This Program

This section outlines the steps required to run the Spring Boot application and how to set up a Redis instance using Docker.


---


### **1. Prerequisites**


Before running the program, make sure you have the following installed on your system:


- **Java 11 or higher**: To build and run the Spring Boot application.
- **Maven**: To build the Spring Boot project.
- **Docker**: To spin up the Redis instance locally.
- **Git**: To clone the repository.


### **2. Clone the Repository**


First, clone the project repository to your local machine:


```bash
git clone https://github.com/sardul3/io-api-best-practices-boot.git
cd io-api-best-practices-boot
```


### **3. Run Redis Using Docker**


We will use Docker to spin up a Redis instance. If Docker is installed, run the following command to start a Redis container:


```bash
docker run --name redis-cache -p 6379:6379 -d redis:latest
```


- This command pulls the latest Redis image, starts the container, and exposes Redis on port `6379`, which is the default Redis port.


- To verify that Redis is running, use the following command:


```bash
docker ps
```


- You should see a running Redis container in the output. To check Redis logs, run:


```bash
docker logs redis-cache
```


### **4. Update the `application.yml` (If Required)**


The Redis configuration in the Spring Boot application is located in `src/main/resources/application.yml`. By default, the app is configured to connect to Redis running on `localhost` at port `6379`. If you are using a custom Redis configuration, make sure to update the connection properties in the `application.yml` file:


```yaml
spring:
  redis:
    host: localhost
    port: 6379
```


### **5. Build and Run the Application**


Now that Redis is running, you can build and run the Spring Boot application.


#### Build the application:


```bash
mvn clean install
```


#### Run the application:


```bash
mvn spring-boot:run
```


This will start the Spring Boot application, which will connect to the Redis instance running in Docker.


### **6. Access the Endpoints**


Once the application is running, you can access the following endpoints:


1. **Generate Idempotency Key**:  
   This endpoint generates a server-side idempotency key that can be used in subsequent API requests.


   ```bash
   curl -X GET http://localhost:8080/api/server-generated/generate-key
   ```


2. **Process Payment**:  
   Use the generated idempotency key to process a payment.


   ```bash
   curl -X POST http://localhost:8080/api/payment \
        -H "Idempotency-Key: YOUR_GENERATED_KEY" \
        -H "Content-Type: application/json" \
        -d '{"amount": "100", "currency": "USD"}'
   ```


3. **Get Payment Status**:  
   Retrieve the payment status using the same idempotency key.


   ```bash
   curl -X GET http://localhost:8080/api/payment-status \
        -H "Idempotency-Key: YOUR_GENERATED_KEY"
   ```


4. **Check Redis Health**:  
   Check if the application is successfully connected to Redis.


   ```bash
   curl -X GET http://localhost:8080/api/health/redis
   ```


5. **Get Value for Key**:  
   Retrieve the value for a specific Redis key.


   ```bash
   curl -X GET http://localhost:8080/api/health/redis/key/YOUR_KEY
   ```


6. **Get Redis Stats**:  
   Get the Redis server statistics, such as memory usage, total keys, and more.


   ```bash
   curl -X GET http://localhost:8080/api/health/redis/stats
   ```


### **7. Stop the Redis Container**


Once you are done testing, you can stop and remove the Redis container by running the following commands:


```bash
docker stop redis-cache
docker rm redis-cache
```


This will stop the Redis instance and remove the container.
