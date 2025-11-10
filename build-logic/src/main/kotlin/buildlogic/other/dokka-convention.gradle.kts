

import org.gradle.kotlin.dsl.invoke
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.dokka")
}

tasks {
    withType<Jar>().configureEach {
        if (archiveClassifier.get() == "javadoc") {
            from(named("dokkaHtml").map { (it as DokkaTask).outputDirectory })
        }
    }
}
