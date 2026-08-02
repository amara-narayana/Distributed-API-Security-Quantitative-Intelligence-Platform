FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "target/api-security-platform-0.0.1-SNAPSHOT.jar"]
