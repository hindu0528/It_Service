# --- Stage 1: Build the JAR ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Compile and package the application (leaving build tools behind)
RUN mvn clean package -DskipTests

# --- Stage 2: Lightweight Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a secure, non-root user and group
RUN addgroup -S spring && adduser -S spring -G spring

# Copy only the compiled JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set the container to run as the non-root user
USER spring

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]