# eTags
eTags, or **entity tags**, are a mechanism used in HTTP for web caching and conditional requests to optimize the transfer of resources between a client and a server. They are typically used to validate whether a cached version of a resource is still valid or needs to be refreshed, thereby saving network bandwidth. However, while eTags reduce the amount of data transferred across the network, they do not directly save CPU time for the server.

### How eTags Work
1. **Generation of eTags**: When a client requests a resource (like a web page or an image), the server generates an eTag, which is a unique identifier (usually a hash) based on the content of the resource. The server then sends this eTag along with the resource to the client.
2. **Client-Side Caching**: The client caches both the resource and the associated eTag locally.
3. **Conditional Requests**: On subsequent requests, the client includes the eTag in the `If-None-Match` HTTP header, asking the server whether the resource has changed.
4. **Server-Side Validation**: The server compares the client's eTag with the current version of the resource. If the eTag matches, the server responds with a `304 Not Modified` status, indicating that the resource hasn’t changed, and the client should continue using the cached version.

- If the eTag does not match, the server responds with a `200 OK` status and sends the updated resource with a new eTag.

### **How eTags Save Network Bandwidth**
- **Reduced Data Transfer**: If the resource has not changed (i.e., the eTag is the same), the server doesn't need to send the resource again. Instead, it only sends a small `304 Not Modified` response. This significantly reduces the amount of data transmitted, especially for large resources like images, videos, or documents.

### **Why eTags Don’t Save CPU Time**
Although eTags reduce network traffic, they don’t directly save CPU time for the following reasons:
1. **eTag Calculation**: Generating an eTag usually involves computing a hash or checksum (e.g., MD5, SHA-1, or a custom algorithm) based on the resource’s content. This process requires server-side CPU time. Even though the server may not send the resource again, it still needs to calculate or retrieve the current eTag for comparison.
2. **Resource Access and Validation**: The server often still needs to read the resource (file, database entry, or another asset) from storage to verify if the content has changed. Accessing and verifying this data consumes CPU cycles, even if the resource isn’t transmitted to the client.
3. **Comparison Logic**: The server has to process the request, compare the provided eTag with the current eTag, and decide whether to send a `304 Not Modified` response or the full resource. This conditional logic requires some level of CPU utilization.
4. **Other Overheads**: Serving an HTTP request involves various steps such as parsing headers, handling routing, applying authentication or security measures, and managing other server-side processes. These operations require CPU resources regardless of whether the full resource is returned or just a `304 Not Modified` response.

### **README.md: A Guide to Implementing eTag and Caching in Spring Boot**


---


# **API Best Practices: Implementing eTag and Caching in Spring Boot**


This repository demonstrates how to implement **eTag** (Entity Tag) and **caching** in a Spring Boot application using Redis as the cache provider. It includes an example use case for handling transactions between accounts and optimizing API performance by utilizing both eTags for response validation and Redis caching for reducing database load.


## **Overview**


In this guide, you will learn:
1. **How to implement eTags** in Spring Boot to optimize HTTP responses by using `If-None-Match` headers.
2. **How to use Redis for caching** frequently requested data, such as transaction records, using Spring’s `@Cacheable`, `@CachePut`, and `@CacheEvict` annotations.
3. **How to handle updates** efficiently by ensuring both the cache and the eTag are updated whenever a transaction changes.
4. **Testing the application** using a Postman collection, ensuring all relevant caching and eTag scenarios are covered.


---


## **Technologies Used**


- Java 11+
- Spring Boot
- Redis (for caching)
- JPA (for database interactions)
- Postman (for API testing)
- Lombok (for cleaner code)
- Maven (for build and dependency management)


---


## **Steps to Implement eTag and Caching**


### **1. Setting Up Spring Boot and Redis Caching**


**Step 1**: Add Redis dependencies in your `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```


**Step 2**: Enable caching in your Spring Boot application by adding `@EnableCaching` to your main application class:
```java
@SpringBootApplication
@EnableCaching
public class ApiBestPracticesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiBestPracticesApplication.class, args);
    }
}
```


**Step 3**: Configure Redis serialization in your application. This ensures cache entries are serialized as JSON objects.
```java
@Configuration
@EnableCaching
public class RedisDataFormatConfig {


    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```


### **2. Implementing eTag for API Responses**


**eTag** is a mechanism for validating the state of a resource. It helps minimize network bandwidth by enabling the server to return `304 Not Modified` responses when data hasn't changed, thereby skipping the payload download.


**Step 1**: Create an `ETagGenerator` utility class to generate eTags using the MD5 hashing algorithm.


```java
public class ETagGenerator {


    public static String generateETag(List<Transaction> transactions) {
        String combinedTransactions = transactions.stream()
                .map(t -> t.getTransactionId().toString())
                .collect(Collectors.joining()) + transactions.size();
        return getMD5Hash(combinedTransactions);
    }


    public static String generateETagForTransaction(Transaction transaction) {
        String transactionData = transaction.getTransactionId() + transaction.getFromAccount() +
                transaction.getToAccount() + transaction.getAmount() + transaction.getStatus();
        return getMD5Hash(transactionData);
    }


    private static String getMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return byteArrayToHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }


    private static String byteArrayToHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
```


**Step 2**: In your controller methods, generate eTags and include them in the response.


```java
@GetMapping
public ResponseEntity<List<Transaction>> getTransactions(
        @RequestParam(required = false, defaultValue = "") String from,
        @RequestParam(required = false, defaultValue = "") String to,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {


    List<Transaction> transactions = transactionService.getAllTransactions(from, to);
    String eTag = ETagGenerator.generateETag(transactions);


    if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(eTag).build();
    }


    return ResponseEntity.ok().eTag(eTag).body(transactions);
}
```


### **3. Implementing Caching with Redis**


**Step 1**: Use the `@Cacheable` annotation to cache transactions when they are fetched.


```java
@Cacheable(value = "transactionCache", key = "#transactionId", unless = "#result == null")
public Optional<Transaction> getTransactionById(Long transactionId) {
    log.info("Fetching transaction from the database for transactionId: {}", transactionId);
    return transactionRepository.findById(transactionId);
}
```


**Step 2**: Use `@CachePut` in the `updateTransactionStatus` method to update the cache when a transaction is modified.


```java
@CachePut(value = "transactionCache", key = "#transactionId")
public Transaction updateTransactionStatus(Long transactionId, Transaction.Status newStatus) {
    Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);


    if (transactionOpt.isEmpty()) {
        throw new EntityNotFoundException("Transaction not found for id: " + transactionId);
    }


    Transaction transaction = transactionOpt.get();
    transaction.setStatus(newStatus);
    return transactionRepository.save(transaction);
}
```


**Step 3**: Use `@CacheEvict` to remove stale entries when a transaction is deleted or when the cache needs to be cleared.


```java
@CacheEvict(value = "transactionCache", key = "#transactionId")
public void deleteTransaction(Long transactionId) {
    transactionRepository.deleteById(transactionId);
}
```


### **4. Handling Updates for eTags and Cache**


- **eTags**: When a resource (like a transaction) is updated, you should generate a new eTag using the updated state of the transaction. This ensures that subsequent requests will receive the latest data if the eTag has changed.
- **Cache**: When a transaction is updated, you should ensure that the cache is updated using `@CachePut`. If the transaction is deleted, use `@CacheEvict` to remove it from the cache.


---


## **Running the Application**


1. **Start Redis**: Before running the application, ensure that Redis is running. You can start Redis locally using Docker:


```bash
docker run --name redis -d -p 6379:6379 redis
```


2. **Run the Spring Boot Application**:
```bash
mvn spring-boot:run
```


---


## **Testing the Application**


You can use the **Postman collection** provided in this repository to test different scenarios, such as creating, updating, retrieving, and deleting transactions while observing the behavior of eTags and caching.


### **Steps to Test Using Postman**


1. **Import the Postman collection**: Import the `postman_collection.json` file into Postman.
2. **Run the tests**: The collection includes tests for:
    - **Creating a transaction**: Ensures that a transaction can be created.
    - **Fetching transactions with eTag**: Tests how the server responds with `200 OK` or `304 Not Modified` based on the eTag.
    - **Updating a transaction**: Verifies that the transaction is updated and the cache is updated with the new transaction.
    - **Cache hit scenarios**: Tests cache behavior, including checking whether the transaction is fetched from the cache.


### **Example Postman Use Cases**
- **Create Transaction**: Use the `POST /api/transactions` endpoint to create a transaction.
- **Retrieve Transactions**: Use `GET /api/transactions` to retrieve the list of transactions with an eTag.
- **Update Transaction**: Use `PUT /api/transactions/{id}/status` to update the transaction’s status and check if the cache and eTag are updated.
- **Delete Transaction**: Use `DELETE /api/transactions/{id}` and ensure that the cache entry is evicted.


---


## **Common Use Cases for eTag and Caching**


1. **Optimizing API Performance**: By using eTags, the server only sends data if it has been modified, reducing bandwidth usage.
2. **Reducing Database Load**: Redis caching significantly reduces the number of database queries, improving application performance.
3. **Consistency Between Client and Server**: eTags ensure that clients have the latest version of the resource, while caching ensures that frequently accessed data is quickly available.


---


## **Conclusion**


By implementing eTags and Redis caching in your Spring Boot application, you can significantly improve both the **performance** and **scalability** of your APIs. This repository demonstrates best practices for implementing these features, ensuring optimized response times and reduced load on your database.


Feel free to customize and extend this setup to fit your application’s needs.


### Conclusion
In summary, eTags are highly effective at reducing **network bandwidth** usage by avoiding the re-transmission of unchanged resources. However, because the server still needs to perform computational tasks (such as generating eTags, validating resources, and handling the HTTP request), they do not save much **CPU time**. The real benefit of eTags is in optimizing network efficiency rather than server processing efficiency.
