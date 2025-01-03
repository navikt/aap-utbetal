package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsperiode
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsperiodeType
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanBeregner
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class UtbetalingsplanBeregnerTest {

    data class Foo(val num: Int)

    @Test
    fun `En endret og en ny periode`() {
        val start = LocalDate.of(2025, 1, 1)
        val ty1 = opprettTilkjentYtelse(start, 1000, 1000, 1000)
        val ty2 = opprettTilkjentYtelse(start, 1000, 600, 1000, 500)

        val utbetalingsplan = UtbetalingsplanBeregner().tilkjentYtelseTilUtbetalingsplan(ty1, ty2)

        val perioder = utbetalingsplan.perioder
        Assertions.assertThat(perioder.size).isEqualTo(4)
        verifiserUendretPeriode(perioder[0], 1000)
        verifiserEndretPeriode(perioder[1], 600)
        verifiserUendretPeriode(perioder[2], 1000)
        verifiserNyPeriode(perioder[3], 500)
    }

    private fun verifiserUendretPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Long) =
        verifiserPeriode(UtbetalingsperiodeType.UENDRET, utbetalingsperiode, beløp)


    private fun verifiserEndretPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Long) =
        verifiserPeriode(UtbetalingsperiodeType.ENDRET, utbetalingsperiode, beløp)


    private fun verifiserNyPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Long) =
        verifiserPeriode(UtbetalingsperiodeType.NY, utbetalingsperiode, beløp)


    private fun verifiserPeriode(utbetalingsperiodeType: UtbetalingsperiodeType, utbetalingsperiode: Utbetalingsperiode, beløp: Long) {
        assertTrue(utbetalingsperiode.utbetalingsperiodeType == utbetalingsperiodeType)
        assertThat(utbetalingsperiode.utbetaling.dagsats).isEqualTo(Beløp(beløp))
    }


    private fun opprettTilkjentYtelse(startDato: LocalDate, vararg beløpListe: Long): TilkjentYtelse {
        val perioder = beløpListe.mapIndexed { i, beløp ->
            lagTilkjentYtelsePeriode(startDato.plusWeeks(i * 2L), startDato.plusWeeks(i * 2L).plusDays(13), Beløp(beløp))
        }
        return TilkjentYtelse(Saksnummer("123"), UUID.randomUUID(), null, perioder)
    }

    private fun lagTilkjentYtelsePeriode(fom: LocalDate, tom: LocalDate, beløp: Beløp) =
        TilkjentYtelsePeriode(
            periode = Periode(fom, tom),
            detaljer = TilkjentYtelseDetaljer(
                gradering = Prosent.`0_PROSENT`,
                dagsats = beløp,
                grunnlag = beløp,
                grunnbeløp = Beløp(100000L),
                antallBarn = 0,
                barnetillegg = Beløp(0L),
                grunnlagsfaktor = GUnit(BigDecimal.valueOf(0.008)),
                barnetilleggsats = Beløp(36L),
                redusertDagsats = beløp,
            )
        )
}