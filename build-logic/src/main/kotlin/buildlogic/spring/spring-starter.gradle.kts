plugins {
    id("spring-common")
}

dependencies {
    compileOnly("org.springframework:spring-core")
    compileOnly("org.springframework:spring-context")
    // 自动配置
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
//    compileOnly("org.aspectj:aspectjweaver:1.9.9")
    compileOnly("org.aspectj:aspectjweaver")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// 配置 Spring Boot Metadata
tasks.register("generateSpringMetadata") {
    doLast {
        val metadataDir = file("build/resources/main/META-INF")
        metadataDir.mkdirs()

        val springFactories = file("$metadataDir/spring.factories")
        if (!springFactories.exists()) {
            println("Note: Spring factories should be in src/main/resources/META-INF/spring.factories")
        }
    }
}

tasks.named("processResources") {
    dependsOn("generateSpringMetadata")
}
