import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

// Project information (group, version) is defined in gradle.properties

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-autoconfigure:2.4.2")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:2.4.2")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Apache Commons
    implementation("org.apache.commons:commons-collections4:4.4")

    // JetBrains Annotations
    implementation("org.jetbrains:annotations:24.0.1")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.4.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
}

kotlin {
    jvmToolchain(8)
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xjvm-default=all"
            )
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

    javadoc {
        options.encoding = "UTF-8"
        // 兼容 Java 8，不使用 html5
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

// Maven Central Publishing Configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name.set("TDengine ORM Boot Starter")
                description.set("A Spring Boot Starter providing a semi-ORM framework for TDengine time-series database operations")
                url.set("https://github.com/zephyrcicd/tdengine-orm-boot-starter")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("zephyrcicd")
                        name.set("zephyr")
                        url.set("https://github.com/zephyrcicd")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/zephyrcicd/tdengine-orm-boot-starter.git")
                    developerConnection.set("scm:git:ssh://github.com:zephyrcicd/tdengine-orm-boot-starter.git")
                    url.set("https://github.com/zephyrcicd/tdengine-orm-boot-starter/tree/main")
                }
            }
        }
    }

    repositories {
        maven {
            name = "central"
            url = uri("https://central.sonatype.com/api/v1/publisher/deployments/download/")
            credentials {
                username = findProperty("centralUsername") as String? ?: System.getenv("CENTRAL_USERNAME")
                password = findProperty("centralPassword") as String? ?: System.getenv("CENTRAL_PASSWORD")
            }
        }
    }
}

// GPG Signing
signing {
    // 使用环境变量或 gradle.properties 中的配置
    val signingKey = findProperty("signing.keyId") as String? ?: System.getenv("GPG_KEY_ID")
    val signingPassword = findProperty("signing.password") as String? ?: System.getenv("GPG_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
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