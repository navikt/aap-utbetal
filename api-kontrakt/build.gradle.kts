plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

group = "no.nav.aap.utbetal"

val jacksonVersion = "2.22.1"

dependencies {
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    api("no.nav.aap.kelvin:ktor-openapi-generator:2.0.100")
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-utbetal")
            credentials {
                username = "x-access-token"
                // Ligger tilgjengelig i Github Actions
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
