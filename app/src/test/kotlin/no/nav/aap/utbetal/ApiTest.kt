package no.nav.aap.utbetal

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.utbetal.server.DbConfig
import no.nav.aap.utbetal.server.initDatasource
import no.nav.aap.utbetal.server.server
import no.nav.aap.utbetal.test.Fakes
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import org.junit.jupiter.api.AfterAll
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test

class ApiTest {

    @Test
    fun `Ta imot tilkjent ytelse fra førstegangsbehandling`() {
        val tilkjentYtelse = opprettTilkjentYtelse(BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
    }

    private fun opprettTilkjentYtelse(beløp: BigDecimal, startDato: LocalDate): TilkjentYtelseDto {
        val perioder = (0..25).map {
            TilkjentYtelsePeriodeDto(
                fom = startDato.plusWeeks(it * 2L),
                tom = startDato.plusWeeks(it * 2L).plusDays(13),
                TilkjentYtelseDetaljerDto(
                    gradering = BigDecimal.valueOf(0L),
                    dagsats = beløp,
                    grunnlag = beløp,
                    grunnbeløp = BigDecimal.valueOf(100000L) ,
                    antallBarn = 0,
                    barnetillegg = BigDecimal.valueOf(0L),
                    grunnlagsfaktor = BigDecimal.valueOf(0.008),
                    barnetilleggsats = BigDecimal.valueOf(36L),
                )
            )
        }
        return TilkjentYtelseDto(UUID.randomUUID(), null, perioder)
    }

    private fun postTilkjentYtelse(tilkjentYtelse: TilkjentYtelseDto): Unit? {
        return client.post(
            URI.create("http://localhost:8080/tilkjentytelse"),
            PostRequest(body = tilkjentYtelse)
        )
    }


    companion object {
        private val postgres = no.nav.aap.utbetal.postgreSQLContainer()
        private val fakes = Fakes(azurePort = 8081)
        private lateinit var port: Number

        private val dbConfig = DbConfig(
            host = "test",
            port = "test",
            database = postgres.databaseName,
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )

        private val client = RestClient.withDefaultResponseHandler(
            config = ClientConfig(scope = "utbetal"),
            tokenProvider = ClientCredentialsTokenProvider
        )

        // Starter server
        private val server = embeddedServer(Netty, port = 8080) {
            server(dbConfig = dbConfig)
            module(fakes)
        }.start()

        private fun resetDatabase() {
            @Suppress("SqlWithoutWhere")
            initDatasource(dbConfig).transaction {
                it.execute("DELETE FROM TILKJENT_PERIODE")
                it.execute("DELETE FROM TILKJENT_YTELSE")
            }
        }

        /*
        @JvmStatic
        @BeforeAll
        fun beforeall() {
            no.nav.aap.utbetal.ApiTest.Companion.server.start()
            port =
                runBlocking { no.nav.aap.utbetal.ApiTest.Companion.server.engine.resolvedConnectors().filter { it.type == ConnectorType.HTTP }.first().port }
        }
*/
        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}

private fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}