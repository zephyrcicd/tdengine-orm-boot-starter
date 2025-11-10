package gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configJavaToolChain(jdkVersion: String)  {
    val toInt = jdkVersion.toInt()
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(toInt))
        }
    }
}



fun Project.configJunitPlatform()  {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}




/**
 * 配置Java版本兼容性
 * 统一设置sourceCompatibility和targetCompatibility
 */
fun Project.configureJavaCompatibility(jdkVersion: String) {
    val toInt = jdkVersion.toInt()
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.toVersion(toInt)
        targetCompatibility = JavaVersion.toVersion(toInt)
    }
}

fun Project.configureKotlinCompatibility() {
    val the = the<JavaPluginExtension>()
    val toString = the.targetCompatibility.toString()
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict", "-Xjvm-default=all"))
            jvmTarget.set(provider { JvmTarget.fromTarget(toString) })
        }
    }
}


fun Project.configureKotlinToolchain(jdkVersion: String) {
    val toInt = jdkVersion.toInt()
    val the = the<KotlinJvmProjectExtension>()
    the.jvmToolchain (toInt)
}


/**
 * 配置测试任务使用JUnit平台
 */
fun Project.configureJUnitPlatform() {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

/**
 * 配置Kotlin测试依赖
 */
fun Project.configureKotlinTestDependencies() {
    dependencies.apply {
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }
}

/**
 * 配置Java插件扩展以包含源码JAR
 */
fun Project.configureWithSourcesJar() {
    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }
}

fun Project.configUtf8() {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

// Java执行任务编码
    tasks.withType<JavaExec>().configureEach {
        // 添加完整的UTF-8编码支持
        jvmArgs("-Dfile.encoding=UTF-8")
        //保证终端cli打印正确
        jvmArgs("-Dsun.stdout.encoding=UTF-8")
        jvmArgs("-Dsun.stderr.encoding=UTF-8")
        jvmArgs("-Dsun.jnu.encoding=UTF-8")
    }

// Javadoc任务编码
    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }
}
