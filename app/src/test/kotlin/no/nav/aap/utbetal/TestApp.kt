package no.nav.aap.utbetal

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.aap.utbetal.server.DbConfig import no.nav.aap.utbetal.server.server
import no.nav.aap.utbetal.test.AzurePortHolder
import no.nav.aap.utbetal.test.FakeServers
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

fun main() {
    val postgres = postgreSQLContainer()

    AzurePortHolder.setPort(8081)
    FakeServers.start()

    // Starter server
    embeddedServer(Netty, port = 8080) {
        val dbConfig = DbConfig(
            host = "test",
            port = "test",
            database = "test",
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
        server(
            dbConfig
        )
    }.start(wait = true)
}


private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}