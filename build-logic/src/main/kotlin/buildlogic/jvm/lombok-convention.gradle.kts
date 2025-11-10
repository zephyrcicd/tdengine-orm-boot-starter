

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("java-convention")
}


val libs = the<LibrariesForLibs>()
dependencies {
    compileOnly(libs.org.projectlombok.lombok)
    annotationProcessor(libs.org.projectlombok.lombok)

    testCompileOnly(libs.org.projectlombok.lombok)
    testAnnotationProcessor(libs.org.projectlombok.lombok)

}
