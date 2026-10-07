FROM gradle:8.11-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S botgroup && adduser -S botuser -G botgroup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER botuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
