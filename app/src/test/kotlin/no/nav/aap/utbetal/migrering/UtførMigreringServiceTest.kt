package no.nav.aap.utbetal.migrering

import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.GjeldendeAvventPeriodeRepository
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingMappingRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingAvvent
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetalingsperiode
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class UtførMigreringServiceTest {

    private lateinit var dataSource: TestDataSource
    private val utbetalingKlient: UtbetalingKlient = mockk<UtbetalingKlient>()

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
        justRun { utbetalingKlient.migrering(any()) }
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    // ── Per-sak migrering ────────────────────────────────────────────────────

    @Test
    fun `Returnerer tidlig dersom sak allerede er migrert`() {
        val saksnummer = Saksnummer("SAK-001")
        val sakUtbetalingId = dataSource.transaction { connection ->
            SakUtbetalingRepository(connection).lagre(saksnummer, migrertTilKafka = true)
        }

        dataSource.transaction { connection ->
            UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(connection, saksnummer, dryRun = false)

            assertThat(MeldeperiodeUtbetalingMappingRepository(connection).hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)).isEmpty()
            assertThat(GjeldendeAvventPeriodeRepository(connection).hentGjeldendeAvventPeriode(sakUtbetalingId)).isNull()
        }
        verify(exactly = 0) { utbetalingKlient.migrering(any()) }
    }

    @Test
    fun `Kaster IllegalArgumentException dersom sak ikke finnes`() {
        val saksnummer = Saksnummer("FINNES-IKKE")

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            dataSource.transaction { connection ->
                UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(connection, saksnummer, dryRun = false)
            }
        }
    }

    @Test
    fun `dryRun=true gjor ingen skriv til DB og kaller ikke utbetalingKlient`() {
        val saksnummer = Saksnummer("SAK-DRY")
        val sakUtbetalingId = dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val behandlingRef = UUID.randomUUID()
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef)
            opprettBekreftetUtbetalingMedPeriode(
                connection, saksnummer, behandlingRef, tyId, sakUtbetalingId,
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14))
            )
            sakUtbetalingId
        }

        dataSource.transaction { connection ->
            UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(connection, saksnummer, dryRun = true)

            assertThat(MeldeperiodeUtbetalingMappingRepository(connection).hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)).isEmpty()
            assertThat(GjeldendeAvventPeriodeRepository(connection).hentGjeldendeAvventPeriode(sakUtbetalingId)).isNull()
            assertThat(SakUtbetalingRepository(connection).hent(saksnummer)!!.migrertTilKafka).isNull()
        }
        verify(exactly = 0) { utbetalingKlient.migrering(any()) }
    }

    @Test
    fun `dryRun=false oppretter utbetalingStatus, mapping og kaller utbetalingKlient`() {
        val saksnummer = Saksnummer("SAK-FULL")
        val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14))
        val sakUtbetalingId = dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val behandlingRef = UUID.randomUUID()
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef)
            opprettBekreftetUtbetalingMedPeriode(connection, saksnummer, behandlingRef, tyId, sakUtbetalingId, periode)
            sakUtbetalingId
        }

        dataSource.transaction { connection ->
            UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(connection, saksnummer, dryRun = false)
        }

        dataSource.transaction(readOnly = true) { connection ->
            assertThat(MeldeperiodeUtbetalingMappingRepository(connection).hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)).isNotEmpty()
            assertThat(UtbetalingStatusRepository(connection).erAlleUtbetalingerBekreftet(saksnummer)).isTrue()
            assertThat(SakUtbetalingRepository(connection).hent(saksnummer)!!.migrertTilKafka).isNotNull()
        }
        verify(exactly = 1) { utbetalingKlient.migrering(any()) }
    }

    @Test
    fun `Kaster IllegalStateException dersom ikke alle utbetalinger er BEKREFTET`() {
        val saksnummer = Saksnummer("SAK-IKKE-BEKREFTET")
        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val behandlingRef = UUID.randomUUID()
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef)
            opprettUtbetaling(
                connection, saksnummer, behandlingRef, tyId, sakUtbetalingId,
                status = UtbetalingStatus.OPPRETTET,
                perioder = listOf()
            )
        }

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            dataSource.transaction { connection ->
                UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(connection, saksnummer, dryRun = false)
            }
        }.withMessageContaining("bekreftet")
    }

    @Test
    fun `Kaster IllegalStateException ved overlappende perioder`() {
        // UUID1 har perioder [jan 1-10] og [jan 20-31] -> envelope [jan 1, jan 31]
        // UUID2 har periode  [jan 11-19]               -> envelope [jan 11, jan 19]
        // Envelope jan 1-31 overlapper med jan 11-19, som trigger sjekkOmPerioderOverlapper
        val saksnummer = Saksnummer("SAK-OVERLAP")
        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val behandlingRef1 = UUID.randomUUID()
            val behandlingRef2 = UUID.randomUUID()
            val tyId1 = opprettTilkjentYtelse(connection, saksnummer, behandlingRef1)
            val tyId2 = opprettTilkjentYtelse(connection, saksnummer, behandlingRef2, forrigeBehandlingRef = behandlingRef1)

            opprettBekreftetUtbetalingMedPerioder(
                connection, saksnummer, behandlingRef1, tyId1, sakUtbetalingId,
                perioder = listOf(
                    Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10)),
                    Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 1, 31)),
                )
            )
            opprettBekreftetUtbetalingMedPerioder(
                connection, saksnummer, behandlingRef2, tyId2, sakUtbetalingId,
                perioder = listOf(Periode(LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 19)))
            )
        }

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            dataSource.transaction { connection ->
                UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(connection, saksnummer, dryRun = false)
            }
        }.withMessageContaining("overlapper")
    }

    @Test
    fun `Oppretter gjeldende avvent periode korrekt ved avvent-historikk`() {
        val saksnummer = Saksnummer("SAK-AVVENT")
        val avventPeriode = Periode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 14))
        val sakUtbetalingId = dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val behandlingRef = UUID.randomUUID()
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef)
            opprettBekreftetUtbetalingMedPeriode(
                connection, saksnummer, behandlingRef, tyId, sakUtbetalingId,
                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14)),
                avvent = UtbetalingAvvent(
                    fom = avventPeriode.fom,
                    tom = avventPeriode.tom,
                    overføres = null,
                    årsak = AvventÅrsak.AVVENT_AVREGNING,
                )
            )
            sakUtbetalingId
        }

        dataSource.transaction { connection ->
            UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(connection, saksnummer, dryRun = false)
        }

        dataSource.transaction(readOnly = true) { connection ->
            val lagretAvvent = GjeldendeAvventPeriodeRepository(connection).hentGjeldendeAvventPeriode(sakUtbetalingId)
            assertThat(lagretAvvent).isNotNull()
            assertThat(lagretAvvent!!.periode).isEqualTo(avventPeriode)
        }
    }

    // ── Batch-migrering ──────────────────────────────────────────────────────

    @Test
    fun `Returnerer tomt resultat dersom ingen saker til migrering`() {
        val resultat = UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(maxAntall = 10, dryRun = true)

        assertThat(resultat.migrerteSaker).isEmpty()
        assertThat(resultat.feiledeMigreringer).isEmpty()
    }

    @Test
    fun `Respekterer maxAntall ved batch-migrering`() {
        val periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14))
        listOf("BATCH-001", "BATCH-002", "BATCH-003").forEach { sak ->
            val saksnummer = Saksnummer(sak)
            dataSource.transaction { connection ->
                val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
                val behandlingRef = UUID.randomUUID()
                val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef)
                opprettBekreftetUtbetalingMedPeriode(connection, saksnummer, behandlingRef, tyId, sakUtbetalingId, periode)
            }
        }

        val resultat = UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(maxAntall = 2, dryRun = false)

        assertThat(resultat.migrerteSaker).hasSize(2)
    }

    @Test
    fun `Sak med feil havner ikke i migrerteSaker`() {
        val saksnummer = Saksnummer("BATCH-FEIL")
        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            val behandlingRef = UUID.randomUUID()
            val tyId = opprettTilkjentYtelse(connection, saksnummer, behandlingRef)
            opprettUtbetaling(
                connection, saksnummer, behandlingRef, tyId, sakUtbetalingId,
                status = UtbetalingStatus.OPPRETTET,
                perioder = listOf()
            )
        }

        val resultat = UtførMigreringService(dataSource, utbetalingKlient).utførMigrering(maxAntall = 10, dryRun = false)

        assertThat(resultat.migrerteSaker).doesNotContain(saksnummer)
        assertThat(resultat.feiledeMigreringer).contains(saksnummer)
    }

    // ── Hjelpefunksjoner ─────────────────────────────────────────────────────

    private fun opprettSakUtbetaling(connection: DBConnection, saksnummer: Saksnummer): Long =
        SakUtbetalingRepository(connection).lagre(saksnummer, migrertTilKafka = false)

    private fun opprettTilkjentYtelse(
        connection: DBConnection,
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        forrigeBehandlingRef: UUID? = null,
    ): Long =
        TilkjentYtelseRepository(connection).lagreTilkjentYtelse(
            TilkjentYtelse(
                saksnummer = saksnummer,
                behandlingsreferanse = behandlingRef,
                forrigeBehandlingsreferanse = forrigeBehandlingRef,
                personIdent = "12345678901",
                vedtakstidspunkt = LocalDateTime.now(),
                beslutterId = "beslutter",
                saksbehandlerId = "saksbehandler",
                perioder = listOf(),
            )
        )

    private fun opprettUtbetaling(
        connection: DBConnection,
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        tilkjentYtelseId: Long,
        sakUtbetalingId: Long,
        status: UtbetalingStatus,
        perioder: List<Utbetalingsperiode>,
        avvent: UtbetalingAvvent? = null,
    ): Long {
        val utbetaling = Utbetaling(
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingRef,
            tilkjentYtelseId = tilkjentYtelseId,
            personIdent = "12345678901",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "beslutter",
            saksbehandlerId = "saksbehandler",
            utbetalingOversendt = LocalDateTime.now(),
            utbetalingEndret = if (status == UtbetalingStatus.BEKREFTET) LocalDateTime.now() else null,
            utbetalingStatus = UtbetalingStatus.OPPRETTET,
            perioder = perioder,
            avvent = avvent,
            utbetalingRef = UUID.randomUUID(),
        )
        val utbetalingId = UtbetalingRepository(connection).lagre(sakUtbetalingId, utbetaling)
        if (status != UtbetalingStatus.OPPRETTET) {
            val lagret = UtbetalingRepository(connection).hentUtbetaling(utbetalingId)
            UtbetalingRepository(connection).oppdaterStatus(lagret.id!!, lagret.versjon, status)
        }
        return utbetalingId
    }

    private fun opprettBekreftetUtbetalingMedPeriode(
        connection: DBConnection,
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        tilkjentYtelseId: Long,
        sakUtbetalingId: Long,
        periode: Periode,
        avvent: UtbetalingAvvent? = null,
    ): Long = opprettBekreftetUtbetalingMedPerioder(
        connection, saksnummer, behandlingRef, tilkjentYtelseId, sakUtbetalingId,
        perioder = listOf(periode),
        avvent = avvent,
    )

    private fun opprettBekreftetUtbetalingMedPerioder(
        connection: DBConnection,
        saksnummer: Saksnummer,
        behandlingRef: UUID,
        tilkjentYtelseId: Long,
        sakUtbetalingId: Long,
        perioder: List<Periode>,
        avvent: UtbetalingAvvent? = null,
    ): Long = opprettUtbetaling(
        connection, saksnummer, behandlingRef, tilkjentYtelseId, sakUtbetalingId,
        status = UtbetalingStatus.BEKREFTET,
        perioder = perioder.map {
            Utbetalingsperiode(
                periode = it,
                beløp = 500u,
                fastsattDagsats = 500u,
                utbetalingsperiodeType = UtbetalingsperiodeType.NY,
                utbetalingsdato = it.fom.plusDays(14),
            )
        },
        avvent = avvent,
    )
}
