plugins {
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

group = "ua.horuktaras.osbb"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val telegramBotsVersion = "9.5.0"
val testcontainersVersion = "1.20.4"

dependencies {
    // Spring Boot starters (versions managed by Spring Boot BOM via plugin)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // PostgreSQL driver
    runtimeOnly("org.postgresql:postgresql")

    // TelegramBots (9.5.0) - use BOM platform for consistent versioning
    implementation(platform("org.telegram:telegrambots-bom:$telegramBotsVersion"))
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:$telegramBotsVersion")
    implementation("org.telegram:telegrambots-client:$telegramBotsVersion")
    implementation("org.telegram:telegrambots-meta:$telegramBotsVersion")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Djava.util.concurrent.ForkJoinPool.common.parallelism=1")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}
