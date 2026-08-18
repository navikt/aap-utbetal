package no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.helved.UtbetalingMelding
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingProdusent
import no.nav.aap.utbetal.klienter.helved.Simulering
import no.nav.aap.utbetal.klienter.helved.SimulertUtbetaling
import no.nav.aap.utbetal.klienter.helved.Simuleringsperiode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.simulering.SimuleringService
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseAvvent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseTestUtil
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.GjeldendeAvventPeriode
import no.nav.aap.utbetal.utbetaling.GjeldendeAvventPeriodeRepository
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingMappingRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

class SendUtbetalingUtførerTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `normal utbetaling uten avvent sender én melding og setter status SENDT i DB`() {
        val saksnummer = Saksnummer("111111111")
        val behandlingsreferanse = UUID.randomUUID()
        val mockProdusent = mockk<UtbetalingProdusent>(relaxed = true)

        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            lagreTilkjentYtelse(connection, saksnummer, behandlingsreferanse)

            SendUtbetalingUtfører(
                connection = connection,
                utbetalingProdusentFactory = { mockProdusent },
            ).utfør(lagJobbInput(sakUtbetalingId, behandlingsreferanse))
        }

        verify(exactly = 1) { mockProdusent.sendUtbetalingHendelse(any(), any()) }

        dataSource.transaction { connection ->
            val status = UtbetalingStatusRepository(connection).hent(behandlingsreferanse)
            assertThat(status).isNotNull()
            assertThat(status!!.status).isEqualTo(Status.SENDT)
        }
    }

    @Test
    fun `utfør kaster exception når tilkjent ytelse ikke finnes`() {
        val mockProdusent = mockk<UtbetalingProdusent>(relaxed = true)
        val ikkeEksisterendeBehandlingsreferanse = UUID.randomUUID()

        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, Saksnummer("222222222"))

            assertFailsWith<IllegalArgumentException> {
                SendUtbetalingUtfører(
                    connection = connection,
                    utbetalingProdusentFactory = { mockProdusent },
                ).utfør(lagJobbInput(sakUtbetalingId, ikkeEksisterendeBehandlingsreferanse))
            }
        }

        verify(exactly = 0) { mockProdusent.sendUtbetalingHendelse(any(), any()) }
    }

    @Test
    fun `meldeperiode utbetaling mapping oppdateres ved kjøring`() {
        val saksnummer = Saksnummer("333333333")
        val behandlingsreferanse = UUID.randomUUID()
        val mockProdusent = mockk<UtbetalingProdusent>(relaxed = true)

        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            lagreTilkjentYtelse(connection, saksnummer, behandlingsreferanse)

            SendUtbetalingUtfører(
                connection = connection,
                utbetalingProdusentFactory = { mockProdusent },
            ).utfør(lagJobbInput(sakUtbetalingId, behandlingsreferanse))

            val mapping = MeldeperiodeUtbetalingMappingRepository(connection)
                .hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
            assertThat(mapping).isNotEmpty()
        }
    }

    @Test
    fun `avvent settes første gang sender kun én melding og lagrer gjeldende avvent periode`() {
        val saksnummer = Saksnummer("444444444")
        val behandlingsreferanse = UUID.randomUUID()
        val mockProdusent = mockk<UtbetalingProdusent>(relaxed = true)
        val avvent = TilkjentYtelseAvvent(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 31),
            overføres = LocalDate.of(2025, 2, 21),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )

        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            lagreTilkjentYtelse(connection, saksnummer, behandlingsreferanse, avvent)

            // Ingen GjeldendeAvventPeriode i DB - første gang avvent settes
            SendUtbetalingUtfører(
                connection = connection,
                utbetalingProdusentFactory = { mockProdusent },
            ).utfør(lagJobbInput(sakUtbetalingId, behandlingsreferanse))

            val gjeldendeAvventPeriode = GjeldendeAvventPeriodeRepository(connection)
                .hentGjeldendeAvventPeriode(sakUtbetalingId)
            assertThat(gjeldendeAvventPeriode).isNull()
        }

        verify(exactly = 1) { mockProdusent.sendUtbetalingHendelse(any(), any()) }
    }

    @Test
    fun `avvent periode endret med beløpsendring sender feilregistrering før ny utbetaling`() {
        val saksnummer = Saksnummer("555555555")
        val behandlingsreferanse = UUID.randomUUID()
        val meldinger = mutableListOf<Pair<String, UtbetalingMelding>>()
        val mockProdusent = mockk<UtbetalingProdusent>()
        every { mockProdusent.sendUtbetalingHendelse(any(), any()) } answers {
            meldinger.add(firstArg<String>() to secondArg())
        }

        val gammelAvventPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        val nyAvvent = TilkjentYtelseAvvent(
            fom = LocalDate.of(2025, 1, 15),
            tom = LocalDate.of(2025, 2, 15),
            overføres = LocalDate.of(2025, 3, 1),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )

        val mockSimulering = mockk<SimuleringService>()
        every { mockSimulering.simuler(any()) } returns Simulering(
            perioder = listOf(
                Simuleringsperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 14),
                    utbetalinger = listOf(
                        SimulertUtbetaling(
                            sakId = saksnummer.toString(),
                            utbetalesTil = "12345678901",
                            tidligereUtbetalt = 500,
                            nyttBeløp = 600,
                        )
                    )
                )
            )
        )

        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            lagreTilkjentYtelse(connection, saksnummer, behandlingsreferanse, nyAvvent)
            GjeldendeAvventPeriodeRepository(connection).lagre(
                GjeldendeAvventPeriode(sakUtbetalingId, gammelAvventPeriode)
            )

            SendUtbetalingUtfører(
                connection = connection,
                utbetalingProdusentFactory = { mockProdusent },
                simuleringServiceFactory = { _ -> mockSimulering },
            ).utfør(lagJobbInput(sakUtbetalingId, behandlingsreferanse))
        }

        assertThat(meldinger).hasSize(2)
        val (_, feilregistrering) = meldinger[0]
        val (_, normalUtbetaling) = meldinger[1]

        assertThat(feilregistrering.avvent).isNotNull()
        assertThat(feilregistrering.avvent!!.feilregistrering).isTrue()
        assertThat(feilregistrering.avvent.fom).isEqualTo(gammelAvventPeriode.fom.toString())
        assertThat(feilregistrering.avvent.tom).isEqualTo(gammelAvventPeriode.tom.toString())
        assertThat(feilregistrering.utbetalinger).isEmpty()

        assertThat(normalUtbetaling.avvent).isNotNull()
        assertThat(normalUtbetaling.avvent!!.feilregistrering).isFalse()
        assertThat(normalUtbetaling.avvent.fom).isEqualTo(nyAvvent.fom.toString())
        assertThat(normalUtbetaling.avvent.tom).isEqualTo(nyAvvent.tom.toString())
    }

    @Test
    fun `avvent periode endret uten beløpsendring sender kun én melding`() {
        val saksnummer = Saksnummer("666666666")
        val behandlingsreferanse = UUID.randomUUID()
        val mockProdusent = mockk<UtbetalingProdusent>(relaxed = true)

        val gammelAvventPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        val nyAvvent = TilkjentYtelseAvvent(
            fom = LocalDate.of(2025, 1, 15),
            tom = LocalDate.of(2025, 2, 15),
            overføres = LocalDate.of(2025, 3, 1),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )

        val mockSimulering = mockk<SimuleringService>()
        every { mockSimulering.simuler(any()) } returns Simulering(
            perioder = listOf(
                Simuleringsperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 14),
                    utbetalinger = listOf(
                        SimulertUtbetaling(
                            sakId = saksnummer.toString(),
                            utbetalesTil = "12345678901",
                            tidligereUtbetalt = 500,
                            nyttBeløp = 500, // Ingen beløpsendring
                        )
                    )
                )
            )
        )

        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            lagreTilkjentYtelse(connection, saksnummer, behandlingsreferanse, nyAvvent)
            GjeldendeAvventPeriodeRepository(connection).lagre(
                GjeldendeAvventPeriode(sakUtbetalingId, gammelAvventPeriode)
            )

            SendUtbetalingUtfører(
                connection = connection,
                utbetalingProdusentFactory = { mockProdusent },
                simuleringServiceFactory = { _ -> mockSimulering },
            ).utfør(lagJobbInput(sakUtbetalingId, behandlingsreferanse))
        }

        verify(exactly = 1) { mockProdusent.sendUtbetalingHendelse(any(), any()) }
    }

    @Test
    fun `avvent periode uendret sender kun én melding uten å kalle simulering`() {
        val saksnummer = Saksnummer("777777777")
        val behandlingsreferanse = UUID.randomUUID()
        val mockProdusent = mockk<UtbetalingProdusent>(relaxed = true)
        val mockSimulering = mockk<SimuleringService>()

        val avventPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        val avvent = TilkjentYtelseAvvent(
            fom = avventPeriode.fom,
            tom = avventPeriode.tom,
            overføres = LocalDate.of(2025, 2, 21),
            årsak = AvventÅrsak.AVVENT_REFUSJONSKRAV,
        )

        dataSource.transaction { connection ->
            val sakUtbetalingId = opprettSakUtbetaling(connection, saksnummer)
            lagreTilkjentYtelse(connection, saksnummer, behandlingsreferanse, avvent)
            GjeldendeAvventPeriodeRepository(connection).lagre(
                GjeldendeAvventPeriode(sakUtbetalingId, avventPeriode) // Identisk periode
            )

            SendUtbetalingUtfører(
                connection = connection,
                utbetalingProdusentFactory = { mockProdusent },
                simuleringServiceFactory = { _ -> mockSimulering },
            ).utfør(lagJobbInput(sakUtbetalingId, behandlingsreferanse))
        }

        verify(exactly = 1) { mockProdusent.sendUtbetalingHendelse(any(), any()) }
        verify(exactly = 0) { mockSimulering.simuler(any()) }
    }

    private fun opprettSakUtbetaling(connection: DBConnection, saksnummer: Saksnummer): Long {
        return SakUtbetalingRepository(connection).lagre(saksnummer, true)
    }

    private fun lagreTilkjentYtelse(
        connection: DBConnection,
        saksnummer: Saksnummer,
        behandlingsreferanse: UUID,
        avvent: TilkjentYtelseAvvent? = null,
    ) {
        TilkjentYtelseRepository(connection).lagreTilkjentYtelse(
            TilkjentYtelseTestUtil.opprettTilkjentYtelse(
                saksnummer = saksnummer,
                behandlingRef = behandlingsreferanse,
                forrigeBehandlingRef = null,
                antallPerioder = 2,
                beløp = Beløp(500L),
                startDato = LocalDate.of(2025, 1, 1),
                avvent = avvent,
            )
        )
    }

    private fun lagJobbInput(sakUtbetalingId: Long, behandlingsreferanse: UUID): JobbInput =
        JobbInput(SendUtbetalingUtfører)
            .forSak(sakUtbetalingId)
            .medParameter("behandlingsreferanse", behandlingsreferanse.toString())
}
