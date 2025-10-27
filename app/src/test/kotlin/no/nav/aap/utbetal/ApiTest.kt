package no.nav.aap.utbetal

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.ConflictHttpResponseException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.tilgang.NoAuthConfig
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.server.DbConfig
import no.nav.aap.utbetal.server.initDatasource
import no.nav.aap.utbetal.server.prosessering.ProsesseringsJobber
import no.nav.aap.utbetal.server.server
import no.nav.aap.utbetal.simulering.UtbetalingOgSimuleringDto
import no.nav.aap.utbetal.test.Fakes
import no.nav.aap.utbetal.tilkjentytelse.MeldeperiodeDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvventDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseTrekkDto
import no.nav.aap.utbetal.trekk.TrekkRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
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
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ApiTest {

    val dataSource = initDatasource(dbConfig)

    @AfterTest
    fun cleanup() {
        fakes.utbetalinger.clear()
    }

    @Test
    fun `Tilkjent ytelse etter førstegangsbehandling`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)

        val uid = ventPåMotor(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)


        val helvedUtbetaling = fakes.utbetalinger[uid]
        assertThat(helvedUtbetaling).isNotNull()
        assertThat(helvedUtbetaling!!.perioder.first().fastsattDagsats).isEqualTo(500.toUInt())
        assertThat(helvedUtbetaling.perioder.first().beløp).isEqualTo(500.toUInt())
        assertThat(helvedUtbetaling.avvent).isNull()

    }

    @Test
    fun `Tilkjent ytelse etter førstegangsbehandling med avvent refusjonskrav`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        val avventDto = TilkjentYtelseAvventDto(
            fom = LocalDate.of(2024, 12, 1),
            tom = LocalDate.of(2024, 12, 31),
            overføres = LocalDate.of(2025, 1, 21),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV
        )
        val tilkjentYtelseMedAvvent = tilkjentYtelse.copy(avvent = avventDto)
        postTilkjentYtelse(tilkjentYtelseMedAvvent)

        val uid = ventPåMotor(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)

        val helvedUtbetaling = fakes.utbetalinger[uid]
        assertThat(helvedUtbetaling).isNotNull()
        assertThat(helvedUtbetaling!!.avvent).isNotNull()
        assertThat(helvedUtbetaling.avvent!!.fom).isEqualTo(avventDto.fom)
        assertThat(helvedUtbetaling.avvent.tom).isEqualTo(avventDto.tom)
        assertThat(helvedUtbetaling.avvent.årsak).isEqualTo(avventDto.årsak)
        assertThat(helvedUtbetaling.avvent.overføres).isEqualTo(avventDto.overføres)
        assertThat(helvedUtbetaling.avvent.feilregistrering).isEqualTo(avventDto.feilregistrering)
    }

    @Test
    fun `Dobbel innsending av samme tilkjent ytelse skal gå bra`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
        postTilkjentYtelse(tilkjentYtelse)

        val uid = ventPåMotor(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)

        assertThat(fakes.utbetalinger).hasSize(1)
        val helvedUtbetaling = fakes.utbetalinger[uid]
        assertThat(helvedUtbetaling).isNotNull()
        assertThat(helvedUtbetaling!!.perioder.first().fastsattDagsats).isEqualTo(500.toUInt())
        assertThat(helvedUtbetaling.perioder.first().beløp).isEqualTo(500.toUInt())
        assertThat(helvedUtbetaling.avvent).isNull()
    }

    @Test
    fun `Dobbel innsending av nesten samme tilkjent ytelse skal kaste exception`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
        assertFailsWith<ConflictHttpResponseException> {
            postTilkjentYtelse(tilkjentYtelse.copy(vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt.plusDays(1)))
        }

        val uid = ventPåMotor(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)

        assertThat(fakes.utbetalinger).hasSize(1)
        val helvedUtbetaling = fakes.utbetalinger[uid]
        assertThat(helvedUtbetaling).isNotNull()
        assertThat(helvedUtbetaling!!.perioder.first().fastsattDagsats).isEqualTo(500.toUInt())
        assertThat(helvedUtbetaling.perioder.first().beløp).isEqualTo(500.toUInt())
        assertThat(helvedUtbetaling.avvent).isNull()
    }

    @Test
    fun `Simulering i en førstegangsbehandling`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        val simuleringer = postTilSimulering(tilkjentYtelse)

        assertThat(simuleringer).hasSize(1)
    }

    @Test
    fun `Trekk i utbetaling med endring på meldekort som gjør at posteringer må flyttes`() {
        val saksnummer = Random().nextInt(999999999).toString()

        val tilkjentYtelse = opprettTilkjentYtelse(
            saksnummer,
            LocalDate.parse("2025-10-06"),
            BigDecimal(500L), BigDecimal(600L)
        ).copy(
            nyMeldeperiode = MeldeperiodeDto(LocalDate.parse("2025-10-20"), LocalDate.parse("2025-11-02")),
            trekk = listOf(
                TilkjentYtelseTrekkDto(
                    dato = LocalDate.parse("2025-10-06"),
                    beløp = 1000
                )
            )
        )

        postTilkjentYtelse(tilkjentYtelse)
        val uid = ventPåMotor(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)
        settUtbetalingTilBekreftet(uid)
        val trekkListe = hentTrekk(Saksnummer(tilkjentYtelse.saksnummer))

        assertThat(trekkListe).hasSize(1)
        assertThat(trekkListe.first().posteringer.size).isEqualTo(2)
        val posteringer = trekkListe.first().posteringer.sortedBy { it.dato }
        assertThat(posteringer[0].beløp).isEqualTo(600)
        assertThat(posteringer[0].dato).isEqualTo(LocalDate.parse("2025-10-20"))
        assertThat(posteringer[1].beløp).isEqualTo(400)
        assertThat(posteringer[1].dato).isEqualTo(LocalDate.parse("2025-10-21"))



        val tilkjentYtelse2 = opprettTilkjentYtelse(
            saksnummer,
            LocalDate.parse("2025-10-06"),
            BigDecimal(500L), BigDecimal(0L), BigDecimal(600L)
        ).copy(
            forrigeBehandlingsreferanse = tilkjentYtelse.behandlingsreferanse,
            nyMeldeperiode = MeldeperiodeDto(LocalDate.parse("2025-11-03"), LocalDate.parse("2025-11-15")),
            trekk = listOf(
                TilkjentYtelseTrekkDto(
                    dato = LocalDate.parse("2025-10-06"),
                    beløp = 1000
                )
            )
        )

        postTilkjentYtelse(tilkjentYtelse2)
        val trekkListe2 = hentTrekk(Saksnummer(tilkjentYtelse.saksnummer))

        assertThat(trekkListe2).hasSize(1)
        assertThat(trekkListe2.first().posteringer.size).isEqualTo(2)
        val posteringer2 = trekkListe2.first().posteringer.sortedBy { it.dato }
        assertThat(posteringer2[0].beløp).isEqualTo(600)
        assertThat(posteringer2[0].dato).isEqualTo(LocalDate.parse("2025-11-03"))
        assertThat(posteringer2[1].beløp).isEqualTo(400)
        assertThat(posteringer2[1].dato).isEqualTo(LocalDate.parse("2025-11-04"))

    }


    private fun settUtbetalingTilBekreftet(uid: UUID) {
        dataSource.transaction { connection ->
            connection.execute("update utbetaling set utbetaling_status = 'BEKREFTET' where utbetaling_ref = ?") {
                setParams {
                    setUUID(1, uid)
                }
            }
        }
    }

    private fun hentTilkjentYtelse(behandlingsreferanse: UUID) =
        dataSource.transaction(readOnly = true) {
            TilkjentYtelseRepository(it).hent(behandlingsreferanse)
        }


    private fun hentTrekk(saksnummer: Saksnummer) =
        dataSource.transaction(readOnly = true) {
            TrekkRepository(it).hentTrekk(saksnummer)
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
    private fun opprettTilkjentYtelse(saksnummer: String, startDato: LocalDate, vararg beløpListe: BigDecimal): TilkjentYtelseDto {
        val perioder = beløpListe.mapIndexed { index, beløp ->
            TilkjentYtelsePeriodeDto(
                fom = startDato.plusWeeks(index * 2L),
                tom = startDato.plusWeeks(index * 2L).plusDays(13),
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
                    utbetalingsdato = startDato.plusWeeks(index * 2L).plusDays(14)
                )
            )
        }

        return TilkjentYtelseDto(
            saksnummer = saksnummer,
            behandlingsreferanse = UUID.randomUUID(),
            personIdent = "12345612345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder)
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
            saksnummer = saksnummer,
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

    private fun postTilSimulering(tilkjentYtelse: TilkjentYtelseDto): List<UtbetalingOgSimuleringDto> {
        return client.post(
            URI.create("http://localhost:8080/simulering"),
            PostRequest(body = tilkjentYtelse)
        )!!
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
            server(dbConfig = dbConfig, NoAuthConfig)
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
        @BeforeAll
        fun beforeall() {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

    private fun ventPåMotor(dataSource: DataSource, saksnummer: String, behandlingRef:  UUID): UUID {
        val util = TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron != null }.map { it.type })
        val sakUtbetalingId = dataSource.transaction { connection ->
            SakUtbetalingRepository(connection).hent(Saksnummer(saksnummer))!!.id!!

        }
        util.ventPåSvar(sakUtbetalingId)

        return dataSource.transaction { connection ->
            val utbetalinger = UtbetalingRepository(connection).hent(behandlingRef)
            assertThat(utbetalinger).hasSize(1)
            assertThat(utbetalinger.first().utbetalingStatus).isIn(UtbetalingStatus.SENDT, UtbetalingStatus.BEKREFTET)

            utbetalinger.first().utbetalingRef
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