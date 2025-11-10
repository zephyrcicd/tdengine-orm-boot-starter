rootProject.name = rootDir.name
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
plugins {
    //仓库配置
    id("site.addzero.repo-buddy") version "+"
    //递归识别模块
    id("io.gitee.zjarlin.auto-modules") version "0.0.608"
}

val bdlogic = "build-logic"
autoModules {
    excludeModules = listOf(bdlogic)
}
includeBuild(bdlogic)


//为何要在这里声明?
//build-logic其实可以作为远程复合构建，那么把toml提交到git可以在多项目中复用版本控制 参考https://melix.github.io/includegit-gradle-plugin/latest/index.html#_known_limitations
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./build-logic/gradle/libs.versions.toml"))
        }
    }
}
