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
import no.nav.aap.utbetaling.Endringstype
import no.nav.aap.utbetaling.UtbetalingsperiodeDto
import no.nav.aap.utbetaling.UtbetalingsplanDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.Ignore
import kotlin.test.Test

class ApiTest {

    @Test
    fun `Ta imot tilkjent ytelse fra førstegangsbehandling`() {
        val tilkjentYtelse = opprettTilkjentYtelse(26, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
    }

    @Test
    @Ignore
    fun `Simuler utbetaling i revurdering`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)

        var nesteTilkjentYtelse = opprettTilkjentYtelse(4, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        nesteTilkjentYtelse = nesteTilkjentYtelse.copy(
            forrigeBehandlingsreferanse = tilkjentYtelse.behandlingsreferanse,
            behandlingsreferanse = UUID.randomUUID(),
            perioder = listOf(
                nesteTilkjentYtelse.perioder[0],
                nesteTilkjentYtelse.perioder[1].copy(detaljer = tilkjentYtelse.perioder[1].detaljer.copy(redusertDagsats = BigDecimal(250))),
                nesteTilkjentYtelse.perioder[2],
                nesteTilkjentYtelse.perioder[3]
            )
        )

        val utbetalingsplan = simulerUtbetaling(nesteTilkjentYtelse)

        val simulertePerioder = utbetalingsplan!!.perioder
        assertThat(simulertePerioder).hasSize(4)
        simulertePerioder.sjekkPeriode(0, 500L, Endringstype.UENDRET)
        simulertePerioder.sjekkPeriode(1, 250, Endringstype.ENDRET)
        simulertePerioder.sjekkPeriode(2, 500L, Endringstype.UENDRET)
        simulertePerioder.sjekkPeriode(3, 500L, Endringstype.NY)
    }

    private fun List<UtbetalingsperiodeDto>.sjekkPeriode(index :Int, beløp: Long, endringstype: Endringstype) {
        assertThat(this[index].redusertDagsats).isEqualTo(Beløp(beløp).verdi())
        assertThat(this[index].endringstype).isEqualTo(endringstype)
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
                    grunnbeløp = BigDecimal.valueOf(100000L) ,
                    antallBarn = 0,
                    barnetillegg = BigDecimal.valueOf(0L),
                    grunnlagsfaktor = BigDecimal.valueOf(0.008),
                    barnetilleggsats = BigDecimal.valueOf(36L),
                    redusertDagsats = beløp,
                )
            )
        }
        val saksnummer = Random().nextInt(999999999).toString()
        return TilkjentYtelseDto("$saksnummer", UUID.randomUUID(), null, perioder)
    }

    private fun postTilkjentYtelse(tilkjentYtelse: TilkjentYtelseDto): Unit? {
        return client.post(
            URI.create("http://localhost:8080/tilkjentytelse"),
            PostRequest(body = tilkjentYtelse)
        )
    }

    private fun simulerUtbetaling(tilkjentYtelse: TilkjentYtelseDto): UtbetalingsplanDto? {
        return client.post(
            URI.create("http://localhost:8080/simulering"),
            PostRequest(body = tilkjentYtelse)
        )
    }

    companion object {
        private val postgres = no.nav.aap.utbetal.postgreSQLContainer()
        private val fakes = Fakes(azurePort = 8081)

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
                it.execute("DELETE FROM UTBETALING_PERIODE")
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