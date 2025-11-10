
import gradle.configJavaToolChain
import gradle.configJunitPlatform
import gradle.configUtf8
import gradle.configureJavaCompatibility
import gradle.configureWithSourcesJar
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `java-library`
}

val libs = the<LibrariesForLibs>()
val javaVersion = libs.versions.jdk.get()
configureWithSourcesJar()
configUtf8()
configureJavaCompatibility(javaVersion)
configJavaToolChain(javaVersion)
configJunitPlatform()

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}
