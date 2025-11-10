

import org.gradle.accessors.dm.LibrariesForLibs


plugins {
    id("kotlin-convention")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}
val value = the<LibrariesForLibs>()

dependencies {
    val version = value.versions.springBootGradlePlugin.get()
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$version"))
}

