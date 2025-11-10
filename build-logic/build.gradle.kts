plugins {
    `kotlin-dsl`
}
repositories {
    mavenLocal()
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    gradleApi()
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.com.diffplug.spotless.com.diffplug.spotless.gradle.plugin)
    implementation(libs.gradlePlugin.dokka)
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.org.graalvm.buildtools.native.gradle.plugin)
    implementation(libs.gradlePlugin.mavenPublish)
    implementation(libs.gradlePlugin.dependencyManagement)
    implementation(libs.gradlePlugin.springBoot)
    implementation(libs.gradlePlugin.kotlinSpring)
    implementation(libs.gradlePlugin.ksp)


}
