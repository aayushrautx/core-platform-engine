plugins {
    `java-library`
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
    }
}

dependencies {
    // Spring Boot (auto-configuration support, NOT the runnable plugin)
    api(libs.spring.boot.starter)
    annotationProcessor(libs.spring.boot.autoconfigure.processor)

    // Security
    api(libs.spring.security.core)

    // Redis (Lettuce)
    api(libs.spring.boot.starter.data.redis)

    // AOP for @Authorize aspect
    api(libs.spring.boot.starter.aop)

    // JSON serialization for cache entries and policy conditions
    implementation(libs.jackson.databind)

    // L1 in-memory cache
    implementation(libs.caffeine)

    // Circuit breaker for Redis
    implementation(libs.resilience4j.spring.boot3)

    // Metrics
    implementation(libs.micrometer.core)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Test
    testImplementation(libs.spring.boot.starter.test)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
