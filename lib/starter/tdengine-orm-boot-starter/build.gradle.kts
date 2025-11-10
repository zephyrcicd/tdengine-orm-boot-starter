plugins {
    id("spring-starter")
    id("lombok-convention")
    id("publish-convention")
}
dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:2.4.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    // Apache Commons
    implementation("org.apache.commons:commons-collections4:4.4")
    // JetBrains Annotations
    implementation("org.jetbrains:annotations:24.0.1")
}

