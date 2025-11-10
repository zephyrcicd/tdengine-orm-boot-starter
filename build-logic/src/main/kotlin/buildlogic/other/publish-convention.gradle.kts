plugins {
    id("com.vanniktech.maven.publish")
}
mavenPublishing {
//    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set("TDengine ORM Boot Starter")
        description.set("A Spring Boot Starter providing a semi-ORM framework for TDengine time-series database operations")
        inceptionYear.set("2024")
        url.set("https://github.com/zephyrcicd/tdengine-orm-boot-starter")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("zephyrcicd")
                name.set("zephyr")
                url.set("https://github.com/zephyrcicd")
            }
        }

        scm {
            url.set("https://github.com/zephyrcicd/tdengine-orm-boot-starter")
            connection.set("scm:git:git://github.com/zephyrcicd/tdengine-orm-boot-starter.git")
            developerConnection.set("scm:git:ssh://git@github.com/zephyrcicd/tdengine-orm-boot-starter.git")
        }
    }
}

