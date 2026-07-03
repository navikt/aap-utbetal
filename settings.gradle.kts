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
    // Felles for alle gradle prosjekter i repoet
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
        mavenLocal()
    }
}
