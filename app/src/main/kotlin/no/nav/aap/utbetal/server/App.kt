package no.nav.aap.utbetal.server

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.utbetal.server.prosessering.OpprettUtbetalingUtfører
import no.nav.aap.utbetal.server.prosessering.OverførTilØkonomiJobbUtfører
import no.nav.aap.utbetal.server.prosessering.SjekkForNyeUtbetalingerUtfører
import no.nav.aap.utbetal.server.prosessering.SjekkKvitteringFraØkonomiUtfører
import no.nav.aap.utbetal.tilkjentytelse.tilkjentYtelse
import no.nav.aap.utbetal.utbetaling.hent
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private const val ANTALL_WORKERS = 4

class App

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun PrometheusMeterRegistry.uhåndtertExceptionTeller(type: String): Counter =
    this.counter("behandlingsflyt_uhaandtert_exception_total", listOf(Tag.of("type", type)))


fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
        prometheus.uhåndtertExceptionTeller(e::class.java.name).increment()
    }
    embeddedServer(Netty, 8080) { server(DbConfig()) }.start(wait = true)
}

internal fun Application.server(dbConfig: DbConfig) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    commonKtorModule(prometheus, AzureConfig(), InfoModel(title = "AAP - Utbetling",
        description = """
                For å teste API i dev, besøk
                <a href="https://azure-token-generator.intern.dev.nav.no/api/m2m?aud=dev-gcp:aap:utbetal">Token Generator</a> for å få token.
                
                For å test lokalt:
                <pre>curl -s -XPOST http://localhost:8081/token  | jq -r '.access_token' | pbcopy</pre>
                """.trimIndent()
        )
    )

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            LoggerFactory.getLogger(App::class.java)
                .warn("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
            call.respond(status = HttpStatusCode.Companion.InternalServerError, message = ErrorRespons(cause.message))
        }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)

    val motor = motor(dataSource)

    routing {
        authenticate(AZURE) {
            apiRouting {
                tilkjentYtelse(dataSource, prometheus)
                hent(dataSource, prometheus)
                motorApi(dataSource)
            }
        }
        actuator(prometheus, motor)
    }
}


fun Application.motor(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        jobber = listOf(
            OpprettUtbetalingUtfører,
            OverførTilØkonomiJobbUtfører,
            SjekkKvitteringFraØkonomiUtfører,
            SjekkForNyeUtbetalingerUtfører
        )
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }

    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
    }

    return motor
}


class DbConfig(
    val jdbcUrL: String = System.getenv("NAIS_DATABASE_UTBETAL_UTBETAL_JDBC_URL"),
    val database: String = System.getenv("NAIS_DATABASE_UTBETAL_UTBETAL_DATABASE"),
    val username: String = System.getenv("NAIS_DATABASE_UTBETAL_UTBETAL_USERNAME"),
    val password: String = System.getenv("NAIS_DATABASE_UTBETAL_UTBETAL_PASSWORD")
)

fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.jdbcUrL
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
    connectionTestQuery = "SELECT 1"
})

internal data class ErrorRespons(val message: String?)

private fun Routing.actuator(prometheus: PrometheusMeterRegistry, motor: Motor) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }

        get("/live") {
            val status = HttpStatusCode.Companion.OK
            call.respond(status, "Oppe!")
        }

        get("/ready") {
            if (motor.kjører()) {
                val status = HttpStatusCode.OK
                call.respond(status, "Oppe!")
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, "Kjører ikke")
            }
        }
    }
}

