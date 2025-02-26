package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingBeregnerTest {

    @Test
    fun `Bare nye perioder`() {
        val start = LocalDate.of(2025, 1, 1)
        val ty = opprettTilkjentYtelse(start, 1000, 1100, 1200)

        val utbetaling = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(1, ty, null, Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14)))

        val perioder = utbetaling.perioder
        assertThat(perioder.size).isEqualTo(3)
        verifiserNyPeriode(perioder[0], 1000)
        verifiserNyPeriode(perioder[1], 1000)
        verifiserNyPeriode(perioder[2], 1000)
    }

    @Test
    fun `En endret og en ny periode`() {
        val start = LocalDate.of(2025, 1, 1)
        val forrigeTilkjentYtelse = opprettTilkjentYtelse(start, 1000, 1000, 1000)
        val nyTilkjentYtelse = opprettTilkjentYtelse(start, 1000, 1000, 600, 500)

        val utbetaling = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(1, nyTilkjentYtelse, forrigeTilkjentYtelse, Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 25)))

        val perioder = utbetaling.perioder
        assertThat(perioder.size).isEqualTo(9)
        verifiserUendretPeriode(perioder[0], 1000)
        verifiserUendretPeriode(perioder[1], 1000)
        verifiserUendretPeriode(perioder[2], 1000)
        verifiserEndretPeriode(perioder[3], 600)
        verifiserEndretPeriode(perioder[4], 600)
        verifiserEndretPeriode(perioder[5], 600)
        verifiserNyPeriode(perioder[6], 500)
        verifiserNyPeriode(perioder[7], 500)
        verifiserNyPeriode(perioder[8], 500)
    }

    private fun verifiserUendretPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Int) =
        verifiserPeriode(UtbetalingsperiodeType.UENDRET, utbetalingsperiode, beløp)


    private fun verifiserEndretPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Int) =
        verifiserPeriode(UtbetalingsperiodeType.ENDRET, utbetalingsperiode, beløp)


    private fun verifiserNyPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Int) =
        verifiserPeriode(UtbetalingsperiodeType.NY, utbetalingsperiode, beløp)


    private fun verifiserPeriode(utbetalingsperiodeType: UtbetalingsperiodeType, utbetalingsperiode: Utbetalingsperiode, beløp: Int) {
        assertThat(utbetalingsperiode.utbetalingsperiodeType).isEqualTo(utbetalingsperiodeType)
        assertThat(utbetalingsperiode.beløp).isEqualTo(beløp.toUInt())
    }


    private fun opprettTilkjentYtelse(startDato: LocalDate, vararg beløpListe: Long): TilkjentYtelse {
        val perioder = beløpListe.mapIndexed { i, beløp ->
            lagTilkjentYtelsePeriode(startDato.plusWeeks(i * 2L), startDato.plusWeeks(i * 2L).plusDays(13), Beløp(beløp))
        }
        return TilkjentYtelse(
            id = 123L,
            saksnummer = Saksnummer("123"),
            behandlingsreferanse = UUID.randomUUID(),
            forrigeBehandlingsreferanse = null,
            personIdent = "12345612345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder)
    }

    private fun lagTilkjentYtelsePeriode(fom: LocalDate, tom: LocalDate, beløp: Beløp) =
        TilkjentYtelsePeriode(
            periode = Periode(fom, tom),
            detaljer = YtelseDetaljer(
                gradering = Prosent.`0_PROSENT`,
                dagsats = beløp,
                grunnlag = beløp,
                grunnbeløp = Beløp(100000L),
                antallBarn = 0,
                barnetillegg = Beløp(0L),
                grunnlagsfaktor = GUnit(BigDecimal.valueOf(0.008)),
                barnetilleggsats = Beløp(36L),
                redusertDagsats = beløp,
                ventedagerSamordning = false,
            )
        )
}