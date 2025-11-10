plugins {
    java
    `java-library`
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

// Project information (group, version) is defined in gradle.properties

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    // vanniktech 插件会自动处理 sources jar 和 javadoc jar
    // 移除 withSourcesJar() 和 withJavadocJar() 避免冲突
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

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Apache Commons
    implementation("org.apache.commons:commons-collections4:4.4")

    // JetBrains Annotations
    implementation("org.jetbrains:annotations:24.0.1")
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

    javadoc {
        options.encoding = "UTF-8"
        // 兼容 Java 8，不使用 html5
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

// Maven Central Publishing Configuration using vanniktech plugin
mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set("TDengine ORM Boot Starter")
        description.set("A Spring Boot Starter providing a semi-ORM framework for TDengine time-series database operations")
        inceptionYear.set("2024")
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
            url.set("https://github.com/zephyrcicd/tdengine-orm-boot-starter")
            connection.set("scm:git:git://github.com/zephyrcicd/tdengine-orm-boot-starter.git")
            developerConnection.set("scm:git:ssh://git@github.com/zephyrcicd/tdengine-orm-boot-starter.git")
        }
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