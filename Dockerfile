FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S botgroup && adduser -S botuser -G botgroup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER botuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
