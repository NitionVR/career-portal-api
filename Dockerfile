# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:21-jdk-jammy as builder
WORKDIR /app
COPY gradlew ./
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Add build argument
ARG SKIP_TESTS=false

# Conditionally skip tests during build
RUN if [ "$SKIP_TESTS" = "true" ]; then \
      ./gradlew bootJar -x test; \
    else \
      ./gradlew bootJar; \
    fi

# Stage 2: Serve the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
