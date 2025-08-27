# Stage 1: Build the application
FROM maven:3.9.3-eclipse-temurin-17 AS build

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the Spring Boot jar
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Install curl and download Datadog Java agent
RUN apt-get update && apt-get install -y curl && \
    curl -Lo dd-java-agent.jar https://dtdg.co/latest-java-tracer && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the jar with Datadog agent
ENTRYPOINT ["java", "-javaagent:dd-java-agent.jar", "-Ddd.profiling.enabled=true", "-XX:FlightRecorderOptions=stackdepth=256", "-jar", "app.jar"]
