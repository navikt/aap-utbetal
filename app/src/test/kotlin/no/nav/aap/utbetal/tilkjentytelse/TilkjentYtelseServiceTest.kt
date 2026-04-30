package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetalingsperiode
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

class TilkjentYtelseServiceTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `ny tilkjent ytelse uten tidligere utbetalinger gir OK`() {
        dataSource.transaction { connection ->
            val service = TilkjentYtelseService(connection)
            val tilkjentYtelse = lagTilkjentYtelse()

            val resultat = service.håndterNyTilkjentYtelse(tilkjentYtelse)

            assertThat(resultat).isEqualTo(TilkjentYtelseResponse.OK)
        }
    }

    @Test
    fun `tilkjent ytelse med alle utbetalinger bekreftet gir OK`() {
        val saksnummer = Saksnummer("SAK002")
        val behandlingRef1 = UUID.randomUUID()

        dataSource.transaction { connection ->
            val sakUtbetalingId = SakUtbetalingRepository(connection).lagre(saksnummer, false)
            val tilkjentYtelseId = TilkjentYtelseRepository(connection).lagreTilkjentYtelse(
                lagTilkjentYtelse(saksnummer = saksnummer, behandlingsreferanse = behandlingRef1)
            )
            UtbetalingRepository(connection).lagre(
                sakUtbetalingId,
                lagUtbetaling(saksnummer, behandlingRef1, tilkjentYtelseId, UtbetalingStatus.BEKREFTET)
            )
        }

        dataSource.transaction { connection ->
            val service = TilkjentYtelseService(connection)
            val nyTilkjentYtelse = lagTilkjentYtelse(
                saksnummer = saksnummer,
                behandlingsreferanse = UUID.randomUUID(),
                forrigeBehandlingsreferanse = behandlingRef1
            )

            val resultat = service.håndterNyTilkjentYtelse(nyTilkjentYtelse)

            assertThat(resultat).isEqualTo(TilkjentYtelseResponse.OK)
        }
    }

    @Test
    fun `duplikat tilkjent ytelse med samme innhold gir OK`() {
        val saksnummer = Saksnummer("SAK003")
        val behandlingRef = UUID.randomUUID()

        dataSource.transaction { connection ->
            val service = TilkjentYtelseService(connection)
            val tilkjentYtelse = lagTilkjentYtelse(saksnummer = saksnummer, behandlingsreferanse = behandlingRef)

            service.håndterNyTilkjentYtelse(tilkjentYtelse)

            val resultat = service.håndterNyTilkjentYtelse(tilkjentYtelse)

            assertThat(resultat).isEqualTo(TilkjentYtelseResponse.OK)
        }
    }

    @Test
    fun `duplikat tilkjent ytelse med forskjellig innhold gir CONFLICT`() {
        val saksnummer = Saksnummer("SAK004")
        val behandlingRef = UUID.randomUUID()

        dataSource.transaction { connection ->
            val service = TilkjentYtelseService(connection)
            val tilkjentYtelse = lagTilkjentYtelse(saksnummer = saksnummer, behandlingsreferanse = behandlingRef)

            service.håndterNyTilkjentYtelse(tilkjentYtelse)

            val endretTilkjentYtelse = tilkjentYtelse.copy(
                perioder = listOf(
                    lagTilkjentYtelsePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(13),
                        redusertDagsats = Beløp(2000),
                        dagsats = Beløp(2000),
                    )
                )
            )

            val resultat = service.håndterNyTilkjentYtelse(endretTilkjentYtelse)

            assertThat(resultat).isEqualTo(TilkjentYtelseResponse.CONFLICT)
        }
    }

    @Test
    fun `tilkjent ytelse lagres ved ny behandling`() {
        val saksnummer = Saksnummer("SAK005")
        val behandlingRef = UUID.randomUUID()

        dataSource.transaction { connection ->
            val service = TilkjentYtelseService(connection)
            val tilkjentYtelse = lagTilkjentYtelse(saksnummer = saksnummer, behandlingsreferanse = behandlingRef)

            service.håndterNyTilkjentYtelse(tilkjentYtelse)

            val lagretTilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingRef)
            assertThat(lagretTilkjentYtelse).isNotNull
            assertThat(lagretTilkjentYtelse!!.saksnummer).isEqualTo(saksnummer)
        }
    }

    private fun lagTilkjentYtelse(
        saksnummer: Saksnummer = Saksnummer("SAK999"),
        behandlingsreferanse: UUID = UUID.randomUUID(),
        forrigeBehandlingsreferanse: UUID? = null,
        perioder: List<TilkjentYtelsePeriode> = listOf(
            lagTilkjentYtelsePeriode(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(13),
                redusertDagsats = Beløp(1000),
                dagsats = Beløp(1000),
            )
        )
    ): TilkjentYtelse {
        return TilkjentYtelse(
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingsreferanse,
            forrigeBehandlingsreferanse = forrigeBehandlingsreferanse,
            personIdent = "12345678901",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "beslutter1",
            saksbehandlerId = "saksbehandler1",
            perioder = perioder,
            avvent = null,
        )
    }

    private fun lagTilkjentYtelsePeriode(
        fom: LocalDate,
        tom: LocalDate,
        redusertDagsats: Beløp,
        dagsats: Beløp,
    ): TilkjentYtelsePeriode {
        val periode = Periode(fom, tom)
        return TilkjentYtelsePeriode(
            periode = periode,
            detaljer = YtelseDetaljer(
                redusertDagsats = redusertDagsats,
                gradering = Prosent(100),
                dagsats = dagsats,
                grunnlag = Beløp(300000),
                grunnlagsfaktor = GUnit(6),
                grunnbeløp = Beløp(100000),
                antallBarn = 0,
                barnetilleggsats = Beløp(0),
                barnetillegg = Beløp(0),
                utbetalingsdato = fom.plusDays(14),
                meldeperiode = periode,
                barnepensjonDagsats = Beløp(0),
            )
        )
    }

    private fun lagUtbetaling(
        saksnummer: Saksnummer,
        behandlingsreferanse: UUID,
        tilkjentYtelseId: Long,
        status: UtbetalingStatus,
    ): Utbetaling {
        return Utbetaling(
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingsreferanse,
            tilkjentYtelseId = tilkjentYtelseId,
            personIdent = "12345678901",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "beslutter1",
            saksbehandlerId = "saksbehandler1",
            utbetalingOversendt = LocalDateTime.now(),
            utbetalingStatus = status,
            perioder = listOf(
                Utbetalingsperiode(
                    periode = Periode(LocalDate.now(), LocalDate.now().plusDays(13)),
                    beløp = 1000u,
                    fastsattDagsats = 1000u,
                    utbetalingsperiodeType = UtbetalingsperiodeType.NY,
                    utbetalingsdato = LocalDate.now(),
                )
            ),
            utbetalingRef = UUID.randomUUID(),
        )
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        }
    }
}