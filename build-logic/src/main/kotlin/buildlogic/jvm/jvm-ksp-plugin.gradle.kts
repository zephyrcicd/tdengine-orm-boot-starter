


plugins {
    id("com.google.devtools.ksp")
    id("kotlin-convention")
}

kotlin {
    sourceSets {
        main {
            val string = "build/generated/ksp/main/kotlin"
            kotlin.srcDir(string)
        }
    }

}
