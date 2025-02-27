import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion = "3.1.1"
val komponenterVersjon = "1.0.159"
val behandlingsflytVersjon= "0.0.155"
val utsjekkVersion = "1.0_20241216161508_0b702d7"

plugins {
    id("utbetal.conventions")
    id("io.ktor.plugin") version "3.1.1"
}

application {
    mainClass.set("no.nav.aap.utbetal.server.AppKt")
}

dependencies {
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
    implementation("no.nav:ktor-openapi-generator:1.0.81")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    implementation("no.nav.utsjekk.kontrakter:iverksett:$utsjekkVersion")
    implementation("no.nav.utsjekk.kontrakter:felles:$utsjekkVersion")
    implementation("no.nav.utsjekk.kontrakter:oppdrag:$utsjekkVersion")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.4")
    implementation("ch.qos.logback:logback-classic:1.5.17")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    implementation(project(":dbflyway"))
    implementation(project(":api-kontrakt"))
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.4")
    runtimeOnly("org.postgresql:postgresql:42.7.5")

    testImplementation("com.nimbusds:nimbus-jose-jwt:10.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.testcontainers:postgresql:1.20.5")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        mergeServiceFiles()
    }
}
