package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.trekk.TrekkPostering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class TilkjentYtelsePeriodeSplitterTest {

    @Test
    fun `Ingen trekk posteringer skal føre til ingen endring`() {
        val tilkjentYtelse = lagTilkjentYtelse()
        val oppdatertTilkjentYtelse = TilkjentYtelsePeriodeSplitter.splitt(tilkjentYtelse, emptyList())
        assertThat(oppdatertTilkjentYtelse).isEqualTo(tilkjentYtelse)
    }

    @Test
    fun `Trekk postering skal bli trekt fra dag og splitt opp som egen periode`() {
        val tilkjentYtelse = lagTilkjentYtelse()
        val oppdatertTilkjentYtelse = TilkjentYtelsePeriodeSplitter.splitt(tilkjentYtelse, listOf(
            TrekkPostering(
                id = 1001,
                trekkId = 101,
                dato = LocalDate.parse("2025-10-06"),
                beløp = 1000
            )
        ))

        assertThat(oppdatertTilkjentYtelse.perioder).hasSize(2)

        val periode0 = oppdatertTilkjentYtelse.perioder.first()
        assertThat(periode0.periode.fom).isEqualTo(LocalDate.parse("2025-10-06"))
        assertThat(periode0.periode.tom).isEqualTo(LocalDate.parse("2025-10-06"))
        assertThat(periode0.detaljer.redusertDagsats).isEqualTo(Beløp(0))
        assertThat(periode0.detaljer.trekkPosteringId).isEqualTo(1001)

        val periode1 = oppdatertTilkjentYtelse.perioder.last()
        assertThat(periode1.periode.fom).isEqualTo(LocalDate.parse("2025-10-07"))
        assertThat(periode1.periode.tom).isEqualTo(LocalDate.parse("2025-10-10"))
        assertThat(periode1.detaljer.redusertDagsats).isEqualTo(Beløp(1000))
        assertThat(periode1.detaljer.trekkPosteringId).isNull()
    }

    @Test
    fun `Postering som fører til negativ redusert dagsats skal føre til runtime exception`() {
        val tilkjentYtelse = lagTilkjentYtelse()
        assertThrows<IllegalStateException> {
            TilkjentYtelsePeriodeSplitter.splitt(tilkjentYtelse, listOf(
                TrekkPostering(
                    id = 1001,
                    trekkId = 101,
                    dato = LocalDate.parse("2025-10-06"),
                    beløp = 1100
                )
            ))
        }
    }


    private fun lagTilkjentYtelse(meldeperiode: Periode? = null, trekk: List<TilkjentYtelseTrekk> = emptyList()): TilkjentYtelse {
        return TilkjentYtelse(
            id = 123L,
            saksnummer = Saksnummer("sak123"),
            behandlingsreferanse = UUID.randomUUID(),
            forrigeBehandlingsreferanse = UUID.randomUUID(),
            personIdent = "01017012345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "saksbehandler2",
            saksbehandlerId = "saksbehandler1",
            perioder = listOf(
                TilkjentYtelsePeriode(
                    periode = Periode(LocalDate.parse("2025-10-06"), LocalDate.parse("2025-10-10")),
                    detaljer = YtelseDetaljer(
                        redusertDagsats = Beløp(1000),
                        gradering = Prosent(100),
                        dagsats = Beløp(1000),
                        grunnlag = Beløp(1000),
                        grunnlagsfaktor = GUnit(6),
                        grunnbeløp = Beløp(1000),
                        antallBarn = 0,
                        barnetilleggsats = Beløp(0),
                        barnetillegg = Beløp(0),
                        utbetalingsdato = LocalDate.now(),
                        meldeperiode = meldeperiode,
                    )
                )
            ),
            avvent = null,
            nyMeldeperiode = meldeperiode,
            trekk = trekk
        )
    }

}