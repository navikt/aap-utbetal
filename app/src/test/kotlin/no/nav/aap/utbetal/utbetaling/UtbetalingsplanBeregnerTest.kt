package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsperiode
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanBeregner
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

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

    private fun verifiserUendretPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Long) {
        Assertions.assertThat(utbetalingsperiode).isInstanceOf(Utbetalingsperiode.UendretPeriode::class.java)
        Assertions.assertThat((utbetalingsperiode as Utbetalingsperiode.UendretPeriode).utbetaling.dagsats)
            .isEqualTo(Beløp(beløp))
    }

    private fun verifiserEndretPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Long) {
        Assertions.assertThat(utbetalingsperiode).isInstanceOf(Utbetalingsperiode.EndretPeriode::class.java)
        Assertions.assertThat((utbetalingsperiode as Utbetalingsperiode.EndretPeriode).nyUtbetaling.dagsats)
            .isEqualTo(Beløp(beløp))
    }

    private fun verifiserNyPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Long) {
        Assertions.assertThat(utbetalingsperiode).isInstanceOf(Utbetalingsperiode.NyPeriode::class.java)
        Assertions.assertThat((utbetalingsperiode as Utbetalingsperiode.NyPeriode).utbetaling.dagsats)
            .isEqualTo(Beløp(beløp))
    }

    private fun opprettTilkjentYtelse(startDato: LocalDate, vararg beløpListe: Long): TilkjentYtelseDto {
        val perioder = beløpListe.mapIndexed { i, beløp ->
            lagTilkjentYtelsePeriode(startDato.plusWeeks(i * 2L), startDato.plusWeeks(i * 2L).plusDays(13), beløp)
        }
        return TilkjentYtelseDto(UUID.randomUUID(), null, perioder)
    }

    private fun lagTilkjentYtelsePeriode(fom: LocalDate, tom: LocalDate, beløp: Long) =
        TilkjentYtelsePeriodeDto(
            fom = fom,
            tom = tom,
            TilkjentYtelseDetaljerDto(
                gradering = BigDecimal.valueOf(0L),
                dagsats = BigDecimal.valueOf(beløp),
                grunnlag = BigDecimal.valueOf(beløp),
                grunnbeløp = BigDecimal.valueOf(100000L),
                antallBarn = 0,
                barnetillegg = BigDecimal.valueOf(0L),
                grunnlagsfaktor = BigDecimal.valueOf(0.008),
                barnetilleggsats = BigDecimal.valueOf(36L),
            )
        )
}