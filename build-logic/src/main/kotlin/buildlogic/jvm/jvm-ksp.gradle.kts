
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

plugins {
    id("kotlin-convention")
}
val libs = the<LibrariesForLibs>()
kotlin {
    dependencies {
        implementation(libs.ksp.symbol.processing.api)
    }
}
