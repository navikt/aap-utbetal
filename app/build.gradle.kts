import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(libs.plugins.ktor)
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

    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerAuthJwt)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerCallId)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerMetricsMicrometer)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerStatusPages)

    implementation(libs.ktorSerializationJackson)
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.micrometerPrometheus)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.kafkaClients)

    implementation(project(":dbflyway"))
    implementation(project(":api-kontrakt"))
    implementation(libs.hikari)
    implementation(libs.flywayPostgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.motorTestUtils)
    testImplementation(libs.nimbusJoseJwt)
    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
    testImplementation(libs.assertjCore)
    testImplementation(libs.testcontainersPostgresql)
    testImplementation(libs.testcontainersKafka)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}

tasks {
    withType<ShadowJar> {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
