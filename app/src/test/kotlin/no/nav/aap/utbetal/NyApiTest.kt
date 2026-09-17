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
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.tilgang.NoAuthConfig
import no.nav.aap.utbetal.NyApiTest.Companion.WHITELISTET_FNR
import no.nav.aap.utbetal.NyApiTest.Companion.fakeUtbetalingsmeldingSender
import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusentKonfig
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingProdusent
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.server.DbConfig
import no.nav.aap.utbetal.server.initDatasource
import no.nav.aap.utbetal.server.prosessering.ProsesseringsJobber
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingsmeldingUtfører
import no.nav.aap.utbetal.server.server
import no.nav.aap.utbetal.test.FakeUtbetalingsmeldingSender
import no.nav.aap.utbetal.test.Fakes
import no.nav.aap.utbetal.tilkjentytelse.MeldeperiodeDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvventDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseTrekkDto
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.trekk.TrekkRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingsmeldingType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import no.nav.aap.utbetal.helved.Utbetalingsmelding as HelvedUtbetalingsmelding

/**
 * Tester tilsvarende [ApiTest], men for utbetaling på det nye (Kafka-baserte) grensesnittet.
 *
 * [ApiTest] kan slettes og denne renames til ApiTest når vi har migrert alle saker til nytt grensesnitt.
 *
 * Saker rutes til nytt grensesnitt via et av de whitelistede test-fødselsnumrene i
 * `SjekkMigreringService` (se [WHITELISTET_FNR]). Selve Kafka-sendingen mockes ut med
 * [FakeUtbetalingsmeldingSender] (injisert via [SendUtbetalingsmeldingUtfører.Companion.senderFactory]),
 * slik at testene ikke er avhengig av en ekte Kafka-broker. En eventuell kvittering (OK-status) fra
 * Utsjekk simuleres direkte mot databasen via [UtbetalingStatusRepository], i stedet for å kjøre en
 * ekte Kafka-konsument.
 *
 * "Slett avvent periode"-meldinger går fortsatt over REST (samme fake-server som i [ApiTest]), og
 * fanges opp på samme måte via `fakes.kall`.
 */
class NyApiTest {

    private val dataSource = initDatasource(dbConfig)

    @AfterTest
    fun cleanup() {
        fakes.utbetalinger.clear()
        fakes.kall.clear()
        fakeUtbetalingsmeldingSender.clear()
    }

    @Test
    fun `Tilkjent ytelse etter førstegangsbehandling`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)

        val utbetalingsmelding = ventPåMotorOgBekreft(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)

        assertThat(utbetalingsmelding.utbetalinger).isNotEmpty()
        assertThat(utbetalingsmelding.utbetalinger.first().sats).isEqualTo(500.toUInt())
        assertThat(utbetalingsmelding.utbetalinger.first().utbetaltBeløp).isEqualTo(500.toUInt())
        assertThat(utbetalingsmelding.avvent).isNull()
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

        val utbetalingsmelding = ventPåMotorOgBekreft(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)

        assertThat(utbetalingsmelding.avvent).isNotNull()
        assertThat(utbetalingsmelding.avvent!!.fom).isEqualTo(avventDto.fom.toString())
        assertThat(utbetalingsmelding.avvent.tom).isEqualTo(avventDto.tom.toString())
        assertThat(utbetalingsmelding.avvent.årsak).isEqualTo(avventDto.årsak.toString())
        assertThat(utbetalingsmelding.avvent.overføres).isEqualTo(avventDto.overføres.toString())
        assertThat(utbetalingsmelding.avvent.feilregistrering).isEqualTo(avventDto.feilregistrering)
    }

    @Test
    fun `Dobbel innsending av samme tilkjent ytelse skal gå bra`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
        postTilkjentYtelse(tilkjentYtelse)

        val utbetalingsmelding = ventPåMotorOgBekreft(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)

        val sendtTilDenneBehandlingen = fakeUtbetalingsmeldingSender.sendteMeldinger
            .filter { it.nøkkel == tilkjentYtelse.behandlingsreferanse.toString() }
        assertThat(sendtTilDenneBehandlingen).hasSize(1)
        assertThat(utbetalingsmelding.utbetalinger.first().sats).isEqualTo(500.toUInt())
        assertThat(utbetalingsmelding.utbetalinger.first().utbetaltBeløp).isEqualTo(500.toUInt())
        assertThat(utbetalingsmelding.avvent).isNull()
    }

    @Test
    fun `Dobbel innsending av nesten samme tilkjent ytelse skal kaste exception`() {
        val tilkjentYtelse = opprettTilkjentYtelse(3, BigDecimal(500L), LocalDate.of(2024, 12, 1))
        postTilkjentYtelse(tilkjentYtelse)
        assertFailsWith<ConflictHttpResponseException> {
            postTilkjentYtelse(tilkjentYtelse.copy(vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt.plusDays(1)))
        }

        val utbetalingsmelding = ventPåMotorOgBekreft(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)

        val sendtTilDenneBehandlingen = fakeUtbetalingsmeldingSender.sendteMeldinger
            .filter { it.nøkkel == tilkjentYtelse.behandlingsreferanse.toString() }
        assertThat(sendtTilDenneBehandlingen).hasSize(1)
        assertThat(utbetalingsmelding.utbetalinger.first().sats).isEqualTo(500.toUInt())
        assertThat(utbetalingsmelding.utbetalinger.first().utbetaltBeløp).isEqualTo(500.toUInt())
        assertThat(utbetalingsmelding.avvent).isNull()
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
        ventPåMotorOgBekreft(dataSource, tilkjentYtelse.saksnummer, tilkjentYtelse.behandlingsreferanse)
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
        ventPåMotorOgBekreft(dataSource, tilkjentYtelse2.saksnummer, tilkjentYtelse2.behandlingsreferanse)

        val trekkListe2 = hentTrekk(Saksnummer(tilkjentYtelse.saksnummer))

        assertThat(trekkListe2).hasSize(1)
        assertThat(trekkListe2.first().posteringer.size).isEqualTo(2)
        val posteringer2 = trekkListe2.first().posteringer.sortedBy { it.dato }
        assertThat(posteringer2[0].beløp).isEqualTo(600)
        assertThat(posteringer2[0].dato).isEqualTo(LocalDate.parse("2025-11-03"))
        assertThat(posteringer2[1].beløp).isEqualTo(400)
        assertThat(posteringer2[1].dato).isEqualTo(LocalDate.parse("2025-11-04"))
    }

    @Test
    fun `Endring av avvent-periode skal feilregistrere gammel periode før ny periode sendes`() {
        val saksnummer = Random().nextInt(999999999).toString()
        val startDato = LocalDate.of(2024, 12, 1)

        val gammelAvvent = TilkjentYtelseAvventDto(
            fom = LocalDate.of(2024, 12, 1),
            tom = LocalDate.of(2024, 12, 31),
            overføres = LocalDate.of(2025, 1, 21),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )
        val ty1 = opprettTilkjentYtelse(saksnummer, startDato, BigDecimal(500L)).copy(avvent = gammelAvvent)

        postTilkjentYtelse(ty1)
        ventPåMotorOgBekreft(dataSource, ty1.saksnummer, ty1.behandlingsreferanse)

        val nyAvvent = gammelAvvent.copy(
            fom = LocalDate.of(2024, 11, 15)
        )
        val ty2 = opprettTilkjentYtelse(saksnummer, startDato, BigDecimal(600L)).copy(
            forrigeBehandlingsreferanse = ty1.behandlingsreferanse,
            avvent = nyAvvent,
        )

        postTilkjentYtelse(ty2)
        ventPåMotorOgBekreft(dataSource, ty2.saksnummer, ty2.behandlingsreferanse)

        val feilregistrerte = fakes.kall.filter { it.slettAvvent?.avvent?.feilregistrering == true }
        assertThat(feilregistrerte).hasSize(1)
        val feilregKall = feilregistrerte.single()
        assertThat(feilregKall.metode).isEqualTo("POST")
        assertThat(feilregKall.slettAvvent!!.avvent.fom).isEqualTo(gammelAvvent.fom)
        assertThat(feilregKall.slettAvvent.avvent.tom).isEqualTo(gammelAvvent.tom)
        assertThat(feilregKall.slettAvvent.avvent.årsak).isEqualTo(gammelAvvent.årsak)
        assertThat(feilregKall.slettAvvent.avvent.overføres).isEqualTo(gammelAvvent.overføres)
        assertThat(feilregKall.slettAvvent.avvent.feilregistrering).isTrue()

        val utbetalingsmeldinger = finnUtbetalingsmeldinger(ty2.behandlingsreferanse)
        assertThat(utbetalingsmeldinger).hasSize(2)
        assertThat(utbetalingsmeldinger[0].utbetalingsmeldingType).isEqualTo(UtbetalingsmeldingType.SLETT_AVVENT_PERIODE)
        assertThat(utbetalingsmeldinger[1].utbetalingsmeldingType).isEqualTo(UtbetalingsmeldingType.UTBETALING)
    }

    private data class UtbetalingsmeldingRow(
        val id: Long,
        val utbetalingsmeldingType: UtbetalingsmeldingType,
    )

    private fun finnUtbetalingsmeldinger(behandlingsreferanse: UUID): List<UtbetalingsmeldingRow> {
        return dataSource.transaction { connection ->
            val ty = TilkjentYtelseRepository(connection).hent(behandlingsreferanse)
            val sql = "select id, meldingstype from utbetalingsmelding where tilkjent_ytelse_id = ? order by id"
            connection.queryList(sql) {
                setParams {setLong(1, ty!!.id!!) }
                setRowMapper {
                    UtbetalingsmeldingRow(
                        id = it.getLong("id"),
                        utbetalingsmeldingType = it.getEnum("meldingstype"),
                    )
                }
            }
        }
    }

    @Test
    fun `Uendret avvent-periode skal ikke føre til feilregistrering`() {
        val saksnummer = Random().nextInt(999999999).toString()
        val startDato = LocalDate.of(2024, 12, 1)

        val avvent = TilkjentYtelseAvventDto(
            fom = LocalDate.of(2024, 12, 1),
            tom = LocalDate.of(2024, 12, 31),
            overføres = LocalDate.of(2025, 1, 21),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )
        val ty1 = opprettTilkjentYtelse(saksnummer, startDato, BigDecimal(500L)).copy(avvent = avvent)
        postTilkjentYtelse(ty1)
        ventPåMotorOgBekreft(dataSource, ty1.saksnummer, ty1.behandlingsreferanse)

        val ty2 = opprettTilkjentYtelse(saksnummer, startDato, BigDecimal(600L)).copy(
            forrigeBehandlingsreferanse = ty1.behandlingsreferanse,
            avvent = avvent,
        )
        postTilkjentYtelse(ty2)
        ventPåMotorOgBekreft(dataSource, ty2.saksnummer, ty2.behandlingsreferanse)

        assertThat(fakes.kall.filter { it.utbetaling?.avvent?.feilregistrering == true }).isEmpty()
        assertThat(fakes.kall.filter { it.slettAvvent?.avvent?.feilregistrering == true }).isEmpty()
    }

    @Test
    fun `Første gang avvent settes skal ikke føre til feilregistrering`() {
        val saksnummer = Random().nextInt(999999999).toString()
        val startDato = LocalDate.of(2024, 12, 1)

        val ty1 = opprettTilkjentYtelse(saksnummer, startDato, BigDecimal(500L))
        postTilkjentYtelse(ty1)
        ventPåMotorOgBekreft(dataSource, ty1.saksnummer, ty1.behandlingsreferanse)

        val nyAvvent = TilkjentYtelseAvventDto(
            fom = LocalDate.of(2024, 12, 15),
            tom = LocalDate.of(2025, 1, 15),
            overføres = LocalDate.of(2025, 2, 1),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )
        val ty2 = opprettTilkjentYtelse(saksnummer, startDato, BigDecimal(600L)).copy(
            forrigeBehandlingsreferanse = ty1.behandlingsreferanse,
            avvent = nyAvvent,
        )
        postTilkjentYtelse(ty2)
        ventPåMotorOgBekreft(dataSource, ty2.saksnummer, ty2.behandlingsreferanse)

        assertThat(fakes.kall.filter { it.utbetaling?.avvent?.feilregistrering == true }).isEmpty()
        assertThat(fakes.kall.filter { it.slettAvvent?.avvent?.feilregistrering == true }).isEmpty()
    }

    private fun hentTrekk(saksnummer: Saksnummer) =
        dataSource.transaction(readOnly = true) {
            TrekkRepository(it).hentTrekk(saksnummer)
        }

    /**
     * Tilsvarer [ApiTest] sin `opprettTilkjentYtelse(saksnummer, startDato, vararg beløpListe)`, men
     * bruker et whitelistet test-fnr (ruter saken til nytt grensesnitt) og setter `meldeperiode` på hver
     * periode. `meldeperiode` er påkrevd for at [no.nav.aap.utbetal.helved.tilUtbetalingsmelding] skal
     * kunne bygge en utbetalingsmelding for nytt grensesnitt.
     */
    private fun opprettTilkjentYtelse(saksnummer: String, startDato: LocalDate, vararg beløpListe: BigDecimal): TilkjentYtelseDto {
        val perioder = beløpListe.mapIndexed { index, beløp ->
            val fom = startDato.plusWeeks(index * 2L)
            val tom = fom.plusDays(13)
            TilkjentYtelsePeriodeDto(
                fom = fom,
                tom = tom,
                TilkjentYtelseDetaljerDto(
                    gradering = 100,
                    dagsats = beløp,
                    grunnlag = beløp,
                    grunnbeløp = BigDecimal.valueOf(100000L),
                    antallBarn = 0,
                    barnetillegg = BigDecimal.valueOf(0L),
                    grunnlagsfaktor = BigDecimal.valueOf(0.008),
                    barnepensjonDagsats = BigDecimal.valueOf(0.00),
                    barnetilleggsats = BigDecimal.valueOf(36L),
                    redusertDagsats = beløp,
                    utbetalingsdato = fom.plusDays(14),
                    meldeperiode = MeldeperiodeDto(fom, tom),
                )
            )
        }

        return TilkjentYtelseDto(
            saksnummer = saksnummer,
            behandlingsreferanse = UUID.randomUUID(),
            personIdent = WHITELISTET_FNR,
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder
        )
    }

    /**
     * Tilsvarer [ApiTest] sin `opprettTilkjentYtelse(antallPerioder, beløp, startDato)`, men bruker et
     * whitelistet test-fnr og setter `meldeperiode` på hver periode.
     */
    private fun opprettTilkjentYtelse(antallPerioder: Int, beløp: BigDecimal, startDato: LocalDate): TilkjentYtelseDto {
        val perioder = (0 until antallPerioder).map {
            val fom = startDato.plusWeeks(it * 2L)
            val tom = fom.plusDays(13)
            TilkjentYtelsePeriodeDto(
                fom = fom,
                tom = tom,
                TilkjentYtelseDetaljerDto(
                    gradering = 100,
                    dagsats = beløp,
                    grunnlag = beløp,
                    grunnbeløp = BigDecimal.valueOf(100000L),
                    antallBarn = 0,
                    barnepensjonDagsats = BigDecimal.valueOf(0.00),
                    barnetillegg = BigDecimal.valueOf(0L),
                    grunnlagsfaktor = BigDecimal.valueOf(0.008),
                    barnetilleggsats = BigDecimal.valueOf(36L),
                    redusertDagsats = beløp,
                    utbetalingsdato = fom.plusDays(14),
                    meldeperiode = MeldeperiodeDto(fom, tom),
                )
            )
        }
        val saksnummer = Random().nextInt(999999999).toString()
        return TilkjentYtelseDto(
            saksnummer = saksnummer,
            behandlingsreferanse = UUID.randomUUID(),
            personIdent = WHITELISTET_FNR,
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder
        )
    }

    private fun postTilkjentYtelse(tilkjentYtelse: TilkjentYtelseDto): Unit? {
        return client.post(
            URI.create("http://localhost:8082/tilkjentytelse"),
            PostRequest(body = tilkjentYtelse)
        )
    }

    companion object {
        // Whitelistet test-fnr fra SjekkMigreringService som ruter saken til nytt (Kafka-basert) grensesnitt.
        private const val WHITELISTET_FNR = "29509000997"

        private val postgres = postgreSQLContainer()
        private val fakes = Fakes()
        private val fakeUtbetalingsmeldingSender = FakeUtbetalingsmeldingSender()

        private val dbConfig = DbConfig(
            jdbcUrl = postgres.jdbcUrl,
            database = postgres.databaseName,
            username = postgres.username,
            password = postgres.password
        )

        private val client = RestClient.withDefaultResponseHandler(
            config = ClientConfig(scope = "utbetal"),
            tokenProvider = AzureM2MTokenProvider
        )

        // Starter server
        private val server = embeddedServer(Netty, port = 8082) {
            server(dbConfig = dbConfig, NoAuthConfig)
            module(fakes)
        }.start()

        @JvmStatic
        @BeforeAll
        fun beforeall() {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
            // Mocker ut Kafka-sending for nytt grensesnitt: ingen ekte broker involvert i disse testene.
            SendUtbetalingsmeldingUtfører.senderFactory = { fakeUtbetalingsmeldingSender }
        }

        @AfterTest
        fun resetDatabase() {
            @Suppress("SqlWithoutWhere")
            initDatasource(dbConfig).transaction {
                // UTBETALINGSMELDING/UTBETALING_STATUS(_LINJE) refererer TILKJENT_YTELSE og må
                // slettes før TILKJENT_YTELSE for å unngå FK-brudd.
                it.execute("DELETE FROM UTBETALING_STATUS_LINJE")
                it.execute("DELETE FROM UTBETALING_STATUS")
                it.execute("DELETE FROM UTBETALINGSMELDING")
                it.execute("DELETE FROM TILKJENT_PERIODE")
                it.execute("DELETE FROM TILKJENT_YTELSE")
                it.execute("DELETE FROM UTBETALING_AVVENT")
                it.execute("DELETE FROM UTBETALINGSPERIODE")
                it.execute("DELETE FROM UTBETALING")
                it.execute("DELETE FROM MELDEPERIODE_UTBETALING_MAPPING")
                it.execute("DELETE FROM GJELDENDE_AVVENT_PERIODE")
                it.execute("DELETE FROM SAK_UTBETALING")
            }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            SendUtbetalingsmeldingUtfører.senderFactory = { UtbetalingProdusent(KafkaProdusentKonfig()) }
            server.stop()
            postgres.close()
        }
    }

    /**
     * Venter på at Motor har prosessert jobbene for nytt grensesnitt
     * ([no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.OpprettUtbetalingsmeldingUtfører] og
     * [SendUtbetalingsmeldingUtfører]), bekrefter at status er satt til SENDT, simulerer deretter en
     * OK-kvittering fra Utsjekk direkte mot databasen (i stedet for en ekte Kafka-konsument), og
     * returnerer den utbetalingsmeldingen som ble "sendt" til [fakeUtbetalingsmeldingSender].
     */
    private fun ventPåMotorOgBekreft(dataSource: DataSource, saksnummer: String, behandlingRef: UUID): HelvedUtbetalingsmelding {
        val util = TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron != null }.map { it.type })
        val sakUtbetalingId = dataSource.transaction { connection ->
            SakUtbetalingRepository(connection).hent(Saksnummer(saksnummer))!!.id!!
        }
        util.ventPåSvar(sakUtbetalingId)

        val tilkjentYtelseId = dataSource.transaction(readOnly = true) { connection ->
            val status = UtbetalingStatusRepository(connection).hent(behandlingRef)
            assertThat(status).isNotNull()
            assertThat(status!!.status).isEqualTo(Status.SENDT)
            TilkjentYtelseRepository(connection).hent(behandlingRef)!!.id!!
        }

        // Simuler OK-kvittering fra Utsjekk. I produksjon skjer dette via UtbetalingStatusKonsument som
        // lytter på Kafka-topicet helved.status.v1 - her går vi rett på databasen for å unngå en ekte
        // Kafka-konsument/broker i testen.
        dataSource.transaction { connection ->
            UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatus(
                tilkjentYtelseId = tilkjentYtelseId,
                referanse = behandlingRef,
                utbetalingStatusHendelse = UtbetalingStatusHendelse(
                    status = Status.OK,
                    detaljer = UtbetalingDetaljer(ytelse = "AAP", linjer = listOf())
                )
            )
        }

        dataSource.transaction(readOnly = true) { connection ->
            val status = UtbetalingStatusRepository(connection).hent(behandlingRef)
            assertThat(status).isNotNull()
            assertThat(status!!.status).isEqualTo(Status.OK)
        }

        val sendtMelding = fakeUtbetalingsmeldingSender.sisteMeldingFor(behandlingRef.toString())
        assertThat(sendtMelding)
            .withFailMessage("Fant ingen sendt utbetalingsmelding for behandling $behandlingRef")
            .isNotNull()
        return DefaultJsonMapper.fromJson(sendtMelding!!)
    }

}

private fun postgreSQLContainer(): PostgreSQLContainer {
    val postgres = PostgreSQLContainer("postgres:16")
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
