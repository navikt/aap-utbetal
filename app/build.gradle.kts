import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion = "3.4.0"
val komponenterVersjon = "2.0.10"
val behandlingsflytVersjon = "0.0.561"
val tilgangVersjon = "1.0.180"
val jacksonVersion = "2.21.1"
val jupiterVersjon = "6.0.3"

plugins {
    id("aap.conventions")
    id("io.ktor.plugin") version "3.4.0"
}

application {
    mainClass.set("no.nav.aap.utbetal.server.AppKt")
}

dependencies {
    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:ktor-openapi-generator:$komponenterVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.3")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation(project(":dbflyway"))
    implementation(project(":api-kontrakt"))
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:12.0.2")
    runtimeOnly("org.postgresql:postgresql:42.7.10")

    testImplementation("no.nav.aap.kelvin:motor-test-utils:${komponenterVersjon}")
    testImplementation("com.nimbusds:nimbus-jose-jwt:10.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersjon")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
