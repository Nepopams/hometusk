import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("com.diffplug.spotless") version "6.23.3"
}

group = "com.hometusk"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    // Flyway 9.x (via Spring Boot BOM) does not use modular DB artifacts like flyway-database-postgresql.
    implementation("org.flywaydb:flyway-core")

    // JSON Schema validation
    implementation("com.networknt:json-schema-validator:1.0.87")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Resilience
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

    // Rate limiting
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // Jackson for JSON/JSONB
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Utilities
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")

    // Test utilities
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
    val dockerApiVersion = System.getenv("DOCKER_API_VERSION") ?: "1.44"
    systemProperty("api.version", dockerApiVersion)
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

spotless {
    java {
        target("src/**/*.java")
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        palantirJavaFormat("2.38.0")
    }
}

// Run spotless check before build
tasks.named("check") {
    dependsOn("spotlessCheck")
}
