package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.felles.finnHelger
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*


class UtbetalingBeregnerTest {

    @Test
    fun `Bare nye perioder`() {
        val start = LocalDate.of(2025, 1, 1)
        val ty = opprettTilkjentYtelse(
            startDato = start,
            vedtaksdato =  LocalDate.of(2025, 1, 14).plusDays(9),
            utbetalingsdato = null,
            1000.0, 1100.0, 1200.0
        )

        val utbetalingTidslinje =  Tidslinje<UtbetalingData>()
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(ty, utbetalingTidslinje)

        assertThat(utbetalinger.endringUtbetalinger).hasSize(0)
        assertThat(utbetalinger.nyeUtbetalinger).hasSize(1)
        val nyeUtbetalinger = utbetalinger.nyeUtbetalinger
        val perioder = nyeUtbetalinger[0].perioder
        assertThat(perioder.size).isEqualTo(3)
        verifiserNyPeriode(perioder[0], 1000)
        verifiserNyPeriode(perioder[1], 1000)
        verifiserNyPeriode(perioder[2], 1000)
    }

    @Test
    fun `Skal tillate utbetalingsperioder i inntil 4 uker etter vedtak`() {
        val nå = LocalDate.now()
        val start = nå.minusDays(nå.dayOfWeek.value - 1L) //mandag i denne uken
        val ty = opprettTilkjentYtelse(startDato = start, vedtaksdato =  start, utbetalingsdato = start, 1000.0)

        val utbetalingTidslinje =  Tidslinje<UtbetalingData>()
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(ty, utbetalingTidslinje)

        assertThat(utbetalinger.endringUtbetalinger).hasSize(0)
        assertThat(utbetalinger.nyeUtbetalinger).hasSize(1)
        val nyeUtbetalinger = utbetalinger.nyeUtbetalinger
        val perioder = nyeUtbetalinger[0].perioder
        assertThat(perioder.size).isEqualTo(2)
        verifiserNyPeriode(perioder[0], 1000)
        verifiserNyPeriode(perioder[1], 1000)
    }


    @Test
    fun `Skal hindre utbetaling dersom utbetalingsperioder er over uker etter vedtak`() {
        val nå = LocalDate.now()
        val start = nå.minusDays(nå.dayOfWeek.value - 1L) //mandag i denne uken
        val ty = opprettTilkjentYtelse(startDato = start, vedtaksdato =  start, utbetalingsdato = start, 1000.0, 1000.0, 1000.0)

        val utbetalingTidslinje =  Tidslinje<UtbetalingData>()
        assertThrows<IllegalArgumentException> {
            UtbetalingBeregner().tilkjentYtelseTilUtbetaling(ty, utbetalingTidslinje)
        }
    }


    @Test
    fun `En endret og en ny periode`() {
        val start = LocalDate.of(2025, 1, 1)
        val utbetalingTidslinje = opprettTidslinjeUtbetalinger(start, 1000, 1000, 1000)
        val nyTilkjentYtelse = opprettTilkjentYtelse(
            startDato = start,
            vedtaksdato =  LocalDate.of(2025, 2, 25).plusDays(9),
            utbetalingsdato = null,
            1000.0, 1000.0, 600.0, 500.0)

        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(nyTilkjentYtelse, utbetalingTidslinje)

        assertThat(utbetalinger.endringUtbetalinger).hasSize(1)
        val endringUtbetalingPerioder = utbetalinger.endringUtbetalinger[0].perioder
        assertThat(endringUtbetalingPerioder).hasSize(3)
        verifiserEndretPeriode(endringUtbetalingPerioder[0], 600)
        verifiserEndretPeriode(endringUtbetalingPerioder[1], 600)
        verifiserEndretPeriode(endringUtbetalingPerioder[2], 600)
        assertThat(utbetalinger.nyeUtbetalinger).hasSize(1)
        val nyUtbetalingPerioder = utbetalinger.nyeUtbetalinger[0].perioder
        verifiserNyPeriode(nyUtbetalingPerioder[0], 500)
        verifiserNyPeriode(nyUtbetalingPerioder[1], 500)
        verifiserNyPeriode(nyUtbetalingPerioder[2], 500)
    }


    @Test
    fun `Opphør av en periode`() {
        val start = LocalDate.of(2025, 1, 1)
        val utbetalingTidslinje = opprettTidslinjeUtbetalinger(start, 1000, 1000, 1000)
        val nyTilkjentYtelse = opprettTilkjentYtelse(
            startDato = start,
            vedtaksdato =  LocalDate.of(2025, 2, 25).plusDays(9),
            utbetalingsdato = null,
            1000.0, 0.0, 1000.0)

        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(nyTilkjentYtelse, utbetalingTidslinje)

        assertThat(utbetalinger.endringUtbetalinger).hasSize(1)
        assertThat(utbetalinger.endringUtbetalinger.first().perioder).hasSize(0)
        assertThat(utbetalinger.nyeUtbetalinger).isEmpty()
    }

    @Test
    fun `Opphør i en tidlig periode som ikke er med i tilkjent ytelse lengre`() {
        val start = LocalDate.of(2025, 1, 1)
        val utbetalingTidslinje = opprettTidslinjeUtbetalinger(start.minusDays(14), 1000, 1000, 1000, 1000)
        val nyTilkjentYtelse = opprettTilkjentYtelse(
            startDato = start,
            vedtaksdato =  LocalDate.of(2025, 2, 25).plusDays(9),
            utbetalingsdato = null,
            1000.0, 0.0, 1000.0)

        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(nyTilkjentYtelse, utbetalingTidslinje)

        assertThat(utbetalinger.endringUtbetalinger).hasSize(2)
        assertThat(utbetalinger.endringUtbetalinger[0].perioder).hasSize(0)
        assertThat(utbetalinger.endringUtbetalinger[1].perioder).hasSize(0)
        assertThat(utbetalinger.nyeUtbetalinger).isEmpty()
    }

    private fun verifiserEndretPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Int) =
        verifiserPeriode(UtbetalingsperiodeType.ENDRET, utbetalingsperiode, beløp)


    private fun verifiserNyPeriode(utbetalingsperiode: Utbetalingsperiode, beløp: Int) =
        verifiserPeriode(UtbetalingsperiodeType.NY, utbetalingsperiode, beløp)


    private fun verifiserPeriode(utbetalingsperiodeType: UtbetalingsperiodeType, utbetalingsperiode: Utbetalingsperiode, beløp: Int) {
        assertThat(utbetalingsperiode.utbetalingsperiodeType).isEqualTo(utbetalingsperiodeType)
        assertThat(utbetalingsperiode.beløp).isEqualTo(beløp.toUInt())
    }


    private fun opprettTilkjentYtelse(startDato: LocalDate, vedtaksdato: LocalDate, utbetalingsdato: LocalDate?, vararg beløpListe: Double): TilkjentYtelse {
        val perioder = beløpListe.mapIndexed { i, beløp ->
            lagTilkjentYtelsePeriode(
                fom = startDato.plusWeeks(i * 2L),
                tom = startDato.plusWeeks(i * 2L).plusDays(13),
                utbetalingsdato = utbetalingsdato,
                beløp = Beløp(BigDecimal(beløp))
            )
        }
        return TilkjentYtelse(
            id = 123L,
            saksnummer = Saksnummer("123"),
            behandlingsreferanse = UUID.randomUUID(),
            forrigeBehandlingsreferanse = null,
            personIdent = "12345612345",
            vedtakstidspunkt = LocalDateTime.of(vedtaksdato, LocalTime.MIDNIGHT.plusHours(8)),
            beslutterId = "testbruker1",
            saksbehandlerId = "testbruker2",
            perioder = perioder)
    }

    private fun lagTilkjentYtelsePeriode(fom: LocalDate, tom: LocalDate, utbetalingsdato: LocalDate?, beløp: Beløp) =
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
                utbetalingsdato = utbetalingsdato ?: tom.plusDays(9),
            )
        )


    private fun opprettTidslinjeUtbetalinger(startDato: LocalDate, vararg beløpListe: Int): Tidslinje<UtbetalingData> {
        val segmenter = mutableListOf<Segment<UtbetalingData>>()
        val perioder = beløpListe.mapIndexed { i, beløp ->
            lagUtbetalingData(startDato.plusWeeks(i * 2L), startDato.plusWeeks(i * 2L).plusDays(13), beløp)
        }
        perioder.forEach {
            segmenter.add(Segment(it.first, it.second))
        }
        return fjernHelger(Tidslinje(initSegmenter = segmenter))
    }

    private fun fjernHelger(utbetalinger: Tidslinje<UtbetalingData>): Tidslinje<UtbetalingData> {
        val periode = Periode(
            fom = utbetalinger.perioder().minBy { it.fom }.fom,
            tom = utbetalinger.perioder().maxBy { it.tom }.tom,
        )
        val helger = periode.finnHelger()
        val helgerTidslinje = helger.tilTidslinje()
        return utbetalinger.kombiner(helgerTidslinje, StandardSammenslåere.minus())
    }

    private fun List<Periode>.tilTidslinje() =
        Tidslinje(this.map { periode -> Segment(periode,Unit) })

    private fun lagUtbetalingData(fom: LocalDate, tom: LocalDate, beløp: Int) =
        Pair(
            Periode(fom, tom),
            UtbetalingData(
                utbetalingRef = UUID.randomUUID(),
                beløp = beløp.toUInt(),
                fastsattDagsats = beløp.toUInt(),
                utbetalingsdato = tom.plusDays(9)

            )
        )

}