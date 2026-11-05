# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/mongo-api-1.0.0.jar /app/app.jar
ENV SERVER_PORT=10000
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT:-${SERVER_PORT:-8080}}"]
