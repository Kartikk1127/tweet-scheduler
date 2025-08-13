# ===== Stage 1: Build the application =====
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory inside the container
WORKDIR /app

# Copy the pom.xml and source code into the container
COPY pom.xml .
COPY src ./src

# Package the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# ===== Stage 2: Run the application =====
FROM eclipse-temurin:17-jdk-jammy

# Set working directory for the app
WORKDIR /app

# Copy the packaged jar from the build stage
COPY --from=build /app/target/tweet-scheduler-0.0.1-SNAPSHOT.jar app.jar

# Load environment variables at runtime (Render will inject them)
ENV JAVA_OPTS=""

# Expose port (Spring Boot default is 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]