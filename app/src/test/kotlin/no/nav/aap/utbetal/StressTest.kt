package no.nav.aap.utbetal

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.server.DbConfig
import no.nav.aap.utbetal.server.initDatasource
import no.nav.aap.utbetal.server.server
import no.nav.aap.utbetal.test.Fakes
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import no.nav.aap.utbetaling.UtbetalingsperiodeDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.test.Ignore

@Ignore
class StressTest {

    @Test
    fun `Lagre 1000 tilkjent ytelser, og kjøre alle utbetalinger`() {
        val msLagreTilkjentYtelse = measureTimeMillis {
            (1..1000).forEach { i ->
                val tilkjentYtelse = opprettTilkjentYtelse(26, BigDecimal(500L), LocalDate.of(2024, 12, 1))
                postTilkjentYtelse(tilkjentYtelse)
            }
        }
        println("Lagre 1000  tilkjent ytelser: $msLagreTilkjentYtelse")

        val msOpprettUtbetalingsjobber = measureTimeMillis {
            opprettUtbetalingsjobber()
        }
        println("Oppretter 1000 utbetalingsjobber: $msOpprettUtbetalingsjobber")
    }

    private fun List<UtbetalingsperiodeDto>.sjekkPeriode(index :Int, beløp: Long, utbetalingsperiodeType: UtbetalingsperiodeType) {
        Assertions.assertThat(this[index].redusertDagsats).isEqualTo(Beløp(beløp).verdi())
        Assertions.assertThat(this[index].utbetalingsperiodeType).isEqualTo(utbetalingsperiodeType)
    }

    private fun opprettTilkjentYtelse(antallPerioder: Int, beløp: BigDecimal, startDato: LocalDate): TilkjentYtelseDto {
        val perioder = (0 until antallPerioder).map {
            TilkjentYtelsePeriodeDto(
                fom = startDato.plusWeeks(it * 2L),
                tom = startDato.plusWeeks(it * 2L).plusDays(13),
                TilkjentYtelseDetaljerDto(
                    gradering = BigDecimal.valueOf(0L),
                    dagsats = beløp,
                    grunnlag = beløp,
                    grunnbeløp = BigDecimal.valueOf(100000L),
                    antallBarn = 0,
                    barnetillegg = BigDecimal.valueOf(0L),
                    grunnlagsfaktor = BigDecimal.valueOf(0.008),
                    barnetilleggsats = BigDecimal.valueOf(36L),
                    redusertDagsats = beløp,
                )
            )
        }
        val saksnummer = Random().nextInt(999999999).toString()
        return TilkjentYtelseDto("$saksnummer", UUID.randomUUID(), null, "12345612345", LocalDateTime.now(), "testbruker1", "testbruker2", perioder)
    }

    private fun postTilkjentYtelse(tilkjentYtelse: TilkjentYtelseDto): Unit? {
        return client.post(
            URI.create("http://localhost:8080/tilkjentytelse"),
            PostRequest(body = tilkjentYtelse)
        )
    }

    private fun opprettUtbetalingsjobber(): Unit? {
        return client.post(
            URI.create("http://localhost:8080/opprett-utbetalingsjobber"),
            PostRequest(body = Unit)
        )
    }

    companion object {
        private val postgres = postgreSQLContainer()
        private val fakes = Fakes(azurePort = 8081)

        private val dbConfig = DbConfig(
            host = "test",
            port = "test",
            database = postgres.databaseName,
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )

        private val client = RestClient.Companion.withDefaultResponseHandler(
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

        private fun Application.module(fakes: Fakes) {
            // Setter opp virtuell sandkasse lokalt
            monitor.subscribe(ApplicationStopped) { application ->
                application.environment.log.info("Server har stoppet")
                fakes.close()
                // Release resources and unsubscribe from events
                application.monitor.unsubscribe(ApplicationStopped) {}
            }
        }

        private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
            val postgres = PostgreSQLContainer<Nothing>("postgres:16")
            postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
            postgres.start()
            return postgres
        }

    }


}