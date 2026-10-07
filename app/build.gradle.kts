import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(kelvinLibs.plugins.ktor)
}

application {
    mainClass.set("no.nav.aap.utbetal.server.AppKt")
}

dependencies {
    api(libs.tilgangPlugin)
    implementation(libs.httpklient)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    testImplementation(libs.dbtest)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.motor)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.ktorOpenapiGenerator)
    implementation(libs.behandlingsflytKontrakt)

    implementation(kelvinLibs.ktor.server.auth)
    implementation(kelvinLibs.ktor.server.auth.jwt)
    implementation(kelvinLibs.ktor.server.call.logging)
    implementation(kelvinLibs.ktor.server.call.id)
    implementation(kelvinLibs.ktor.server.content.negotiation)
    implementation(kelvinLibs.ktor.server.metrics.micrometer)
    implementation(kelvinLibs.ktor.server.netty)
    implementation(kelvinLibs.ktor.server.status.pages)

    implementation(kelvinLibs.ktor.serialization.jackson)
    implementation(kelvinLibs.jackson.databind)
    implementation(kelvinLibs.jackson.datatype.jsr310)
    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logback.classic)
    implementation(kelvinLibs.logstash.logback.encoder)
    implementation(kelvinLibs.kafka.clients)

    implementation("no.bekk.bekkopen:nocommons:0.17.0")

    implementation(project(":dbflyway"))
    implementation(project(":api-kontrakt"))
    implementation(kelvinLibs.hikaricp)
    implementation(kelvinLibs.flyway.postgresql)
    runtimeOnly(kelvinLibs.postgresql)

    testImplementation(libs.motorTestUtils)
    testImplementation(kelvinLibs.nimbus.jose.jwt)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kelvinLibs.testcontainers.postgresql)
    testImplementation(kelvinLibs.testcontainers.kafka)
    testImplementation(kelvinLibs.mockk)
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
