package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class MeldeperiodeUtbetalingMappingServiceTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()


    @Test
    fun `lagre og henter meldeperiode til utbetalingId mapping`() {
        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("abc123")
            val periode1 = Periode(LocalDate.parse("2026-03-09"), LocalDate.parse("2026-03-22"))
            val sakUtbetalingId = opprettSakUtbetaling(saksnummer)
            val tilkjentYtelse = lagTilkjentYtelse(
                saksnummer,
                periode1,
            )

            val repo = MeldeperiodeUtbetalingMappingRepository(connection)
            val service = MeldeperiodeUtbetalingMappingService(connection)

            val meldeperiodeUtbetalingMapFørOppdatering = repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
            assertThat(meldeperiodeUtbetalingMapFørOppdatering).isEmpty()

            service.oppdatereMeldeperiodeUtbetalingMapping(
                sakUtbetalingId = sakUtbetalingId,
                tilkjentYtelse = tilkjentYtelse,
            )

            val meldeperiodeUtbetalingMapEtterOppdatering = repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
            assertThat(meldeperiodeUtbetalingMapEtterOppdatering).isNotEmpty()
            assertThat(meldeperiodeUtbetalingMapEtterOppdatering.keys).hasSize(1)
            assertThat(meldeperiodeUtbetalingMapEtterOppdatering.keys.first())
                .isEqualTo(periode1)
        }
    }

    @Test
    fun `lagre og oppdatere meldeperiode til utbetalingId mapping`() {
        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("abc123")
            val periode1 = Periode(LocalDate.parse("2026-03-09"), LocalDate.parse("2026-03-22"))
            val periode2 = Periode(LocalDate.parse("2026-03-23"), LocalDate.parse("2026-04-05"))
            val sakUtbetalingId = opprettSakUtbetaling(saksnummer)
            val tilkjentYtelse1 = lagTilkjentYtelse(
                saksnummer,
                periode1,
            )

            val repo = MeldeperiodeUtbetalingMappingRepository(connection)
            val service = MeldeperiodeUtbetalingMappingService(connection)

            val meldeperiodeUtbetalingMapFørOppdatering = repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
            assertThat(meldeperiodeUtbetalingMapFørOppdatering).isEmpty()

            service.oppdatereMeldeperiodeUtbetalingMapping(
                sakUtbetalingId = sakUtbetalingId,
                tilkjentYtelse = tilkjentYtelse1,
            )

            val meldeperiodeUtbetalingMapEtterOppdatering = repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
            assertThat(meldeperiodeUtbetalingMapEtterOppdatering.keys).hasSize(1)


            val tilkjentYtelse2 = lagTilkjentYtelse(
                saksnummer,
                periode1, periode2
            )

            service.oppdatereMeldeperiodeUtbetalingMapping(
                sakUtbetalingId = sakUtbetalingId,
                tilkjentYtelse = tilkjentYtelse2,
            )

            val meldeperiodeUtbetalingMapEtterEndaEnOppdatering = repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
            assertThat(meldeperiodeUtbetalingMapEtterEndaEnOppdatering.keys).hasSize(2)
        }
    }

    /** Testen kjører fra MeldeperiodeUtbetalingMappingRepositoryTest.kt
     * I MeldeperiodeUtbetalingMappingRepository.kt:42–51 opprettes ingen ny mapping når en utvidet tilkjent periode
     * overlapper en eksisterende mapping. Hvis perioden utvides fra 1.–5. til 1.–10., kan dagene 6.–10. falle stille
     * ut av utbetalingsmeldingen. Mappingene må dekke hele den tilkjente perioden.
     */
    @Test
    fun `oppdatering av periode dekker også siste dag`() {
        dataSource.transaction { connection ->
            val saksnummer = Saksnummer("abc123")
            val førstePeriode = Periode(LocalDate.parse("2026-08-24"), LocalDate.parse("2026-08-25"))
            val utvidetPeriode = Periode(LocalDate.parse("2026-08-24"), LocalDate.parse("2026-08-28"))
            val sakUtbetalingId = opprettSakUtbetaling(saksnummer)

            val repo = MeldeperiodeUtbetalingMappingRepository(connection)
            val service = MeldeperiodeUtbetalingMappingService(connection)


            service.oppdatereMeldeperiodeUtbetalingMapping(
                sakUtbetalingId = sakUtbetalingId,
                tilkjentYtelse = lagTilkjentYtelse(saksnummer, førstePeriode),
            )
            service.oppdatereMeldeperiodeUtbetalingMapping(
                sakUtbetalingId = sakUtbetalingId,
                tilkjentYtelse = lagTilkjentYtelse(saksnummer, utvidetPeriode),
            )

            val mapping = repo.hentMeldeperiodeUtbetalingMapping(sakUtbetalingId)
            assertThat(mapping.keys.any { it.fom <= utvidetPeriode.tom }).isTrue()
            assertThat(mapping.keys.any { it.tom >= utvidetPeriode.tom }).isTrue()
        }
    }

    private fun lagTilkjentYtelse(saksnummer: Saksnummer, vararg perioder: Periode): TilkjentYtelse {
        val tilkjentYtelsePerioder = perioder.map {opprettTilkjentYtelsePeriode(it)}
        return TilkjentYtelse(
            saksnummer = saksnummer,
            behandlingsreferanse = UUID.randomUUID(),
            perioder = tilkjentYtelsePerioder,
            id = 123,
            forrigeBehandlingsreferanse = UUID.randomUUID(),
            personIdent = "12345678910",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "saksbehandling2",
            saksbehandlerId = "saksbehandling1",
            avvent = null,
            nyMeldeperiode = perioder.last(),
            trekk = listOf()
        )

    }

    private fun opprettTilkjentYtelsePeriode(periode: Periode) =
        TilkjentYtelsePeriode(
            periode = periode,
            detaljer = YtelseDetaljer(
                redusertDagsats = Beløp(1000),
                gradering = Prosent(100),
                dagsats = Beløp(1000),
                grunnlagsfaktor = GUnit(1),
                grunnbeløp = Beløp(1000),
                antallBarn = 0,
                barnetilleggsats = Beløp(0),
                barnetillegg = Beløp(0),
                utbetalingsdato = LocalDate.now(),
                trekkPosteringId = null,
                meldeperiode = periode,
                barnepensjonDagsats = Beløp(0)
            )
        )

    private fun opprettSakUtbetaling(saksnummer: Saksnummer): Long {
        return dataSource.transaction { connection ->
            val sakUtbetalingRepo = SakUtbetalingRepository(connection)
            sakUtbetalingRepo.lagre(saksnummer, true)

        }
    }

}