package no.nav.aap.utbetal

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.ConflictHttpResponseException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
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
import java.io.BufferedWriter
import java.io.FileWriter
import java.math.BigDecimal
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ApiTest {

    @Test
    fun `Tilkjent ytelse etter førstegangsbehandling`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
    }

    @Test
    fun `Dobbel innsending av samme tilkjent ytelse skal gå bra`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
        postTilkjentYtelse(tilkjentYtelse)
    }

    @Test
    fun `Dobbel innsending av nesten samme tilkjent ytelse skal kaste exception`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
        assertFailsWith<ConflictHttpResponseException> {
            postTilkjentYtelse(tilkjentYtelse.copy(vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt.plusDays(1)))
        }
    }


    @Test
    fun `Ekporter openapi typer til json`() {
        val openApiDoc =
            requireNotNull(
                client.get(
                    URI.create("http://localhost:8080/openapi.json"),
                    GetRequest()
                ) { body, _ ->
                    String(body.readAllBytes(), StandardCharsets.UTF_8)
                }
            )

        try {
            val writer = BufferedWriter(FileWriter("../openapi.json"))
            writer.write(openApiDoc)

            writer.close()
        } catch (_: Exception) {
            fail()
        }

    }

    private fun opprettTilkjentYtelse(antallPerioder: Int, beløp: BigDecimal, startDato: LocalDate): TilkjentYtelseDto {
        val perioder = (0 until antallPerioder).map {
            TilkjentYtelsePeriodeDto(
                fom = startDato.plusWeeks(it * 2L),
                tom = startDato.plusWeeks(it * 2L).plusDays(13),
                TilkjentYtelseDetaljerDto(
                    gradering = 100,
                    dagsats = beløp,
                    grunnlag = beløp,
                    grunnbeløp = BigDecimal.valueOf(100000L) ,
                    antallBarn = 0,
                    barnetillegg = BigDecimal.valueOf(0L),
                    grunnlagsfaktor = BigDecimal.valueOf(0.008),
                    barnetilleggsats = BigDecimal.valueOf(36L),
                    redusertDagsats = beløp,
                    utbetalingsdato = startDato.plusWeeks(it * 2L).plusDays(14)
                )
            )
        }
        val saksnummer = Random().nextInt(999999999).toString()
        return TilkjentYtelseDto(
            saksnummer = "$saksnummer",
            behandlingsreferanse = UUID.randomUUID(),
            personIdent = "12345612345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder)
    }

    private fun postTilkjentYtelse(tilkjentYtelse: TilkjentYtelseDto): Unit? {
        return client.post(
            URI.create("http://localhost:8080/tilkjentytelse"),
            PostRequest(body = tilkjentYtelse)
        )
    }

    companion object {
        private val postgres = postgreSQLContainer()
        private val fakes = Fakes()

        private val dbConfig = DbConfig(
            jdbcUrL = postgres.jdbcUrl,
            database = postgres.databaseName,
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

        @AfterTest
        fun resetDatabase() {
            @Suppress("SqlWithoutWhere")
            initDatasource(dbConfig).transaction {
                it.execute("DELETE FROM TILKJENT_PERIODE")
                it.execute("DELETE FROM TILKJENT_YTELSE")
                it.execute("DELETE FROM UTBETALING_AVVENT")
                it.execute("DELETE FROM UTBETALINGSPERIODE")
                it.execute("DELETE FROM UTBETALING")
                it.execute("DELETE FROM SAK_UTBETALING")
            }
        }

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