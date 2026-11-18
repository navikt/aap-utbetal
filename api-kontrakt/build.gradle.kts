plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(kelvinLibs.jackson.datatype.jsr310)
    api(libs.ktorOpenapiGenerator)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
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
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
