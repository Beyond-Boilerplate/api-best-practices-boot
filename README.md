# API Best Practices with Idempotency

This project demonstrates various best practices for handling API requests, 

1. **idempotency**. Idempotency ensures that repeated requests with the same parameters produce the same result without causing unintended side effects, making APIs more robust and user-friendly.

    
[Idempotency Documentation](./src/main/java/com/github/sardul3/io/api_best_practices_boot/idempotency/README.md).
2. **eTags and Caching**: eTags help optimize HTTP responses by minimizing bandwidth usage when resources haven't changed, while caching reduces the load on databases by storing frequently requested data in Redis. This section demonstrates how to implement eTags and Redis caching in Spring Boot, how to handle updates, and how to test these optimizations using Postman.

[eTag and Caching Documentation](./src/main/java/com/github/sardul3/io/api_best_practices_boot/eTags/README.md).