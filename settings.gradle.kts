pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "utbetal"

include(
    "app",
    "dbflyway",
    "api-kontrakt"
)

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    versionCatalogs {
        create("kelvinLibs") {
            from("no.nav.aap.kelvin:version-catalog:3.0.3")
        }
    }
    repositories {
        exclusiveContent {
            forRepository {
                maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
            }
            filter {
                includeGroupByRegex("no\\.nav(\\..*)?")
            }
        }
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven("https://packages.confluent.io/maven/")
            }
            filter {
                includeGroupByRegex("io\\.confluent(\\..*)?")
            }
        }
    }
}
