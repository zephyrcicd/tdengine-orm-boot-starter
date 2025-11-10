

import gradle.configureJUnitPlatform
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

import gradle.configureKotlinCompatibility
import gradle.configureKotlinTestDependencies
import gradle.configureKotlinToolchain

plugins {
    kotlin("jvm")
    id("java-convention")
}
val libs = the<LibrariesForLibs>()
val javaVersion = libs.versions.jdk.get()
configureKotlinCompatibility()
configureKotlinToolchain(javaVersion)

configureKotlinTestDependencies()
configureJUnitPlatform()

