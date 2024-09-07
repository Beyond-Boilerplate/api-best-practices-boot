# Use an official OpenJDK runtime, specifically for Java 21
FROM eclipse-temurin:21-jdk-alpine as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradle ./gradle
COPY gradlew ./
COPY build.gradle ./
COPY settings.gradle ./

# Download and cache the project dependencies
RUN ./gradlew build -x test --no-daemon

# Copy the project source code
COPY src ./src

# Build the Spring Boot application using Gradle
RUN ./gradlew clean build -x test --no-daemon

# Use a smaller runtime image with Java 21
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built jar file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port Spring Boot is running on
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
