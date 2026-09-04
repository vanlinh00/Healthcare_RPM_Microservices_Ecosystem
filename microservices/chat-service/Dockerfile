FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY microservices/chat-service/pom.xml microservices/chat-service/
RUN mvn -B dependency:go-offline -pl microservices/chat-service -am || true
COPY microservices/chat-service/src microservices/chat-service/src
RUN mvn -B clean package -pl microservices/chat-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/microservices/chat-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
