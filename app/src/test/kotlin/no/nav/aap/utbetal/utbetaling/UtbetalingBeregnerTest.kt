package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
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
import java.time.LocalTime
import java.util.*

class UtbetalingBeregnerTest {

    @Test
    fun `Bare nye perioder`() {
        val start = LocalDate.of(2025, 1, 1)
        val ty = opprettTilkjentYtelse(startDato = start, vedtaksdato =  LocalDate.of(2025, 1, 14).plusDays(9), 1000, 1100, 1200)

        val utbetalingTidslinje =  Tidslinje<UtbetalingData>()
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(1, ty, utbetalingTidslinje)

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
    fun `En endret og en ny periode`() {
        val start = LocalDate.of(2025, 1, 1)
        val utbetalingTidslinje = opprettTidslinjeUtbetalinger(start, 1000, 1000, 1000)
        val nyTilkjentYtelse = opprettTilkjentYtelse(startDato = start, vedtaksdato =  LocalDate.of(2025, 2, 25).plusDays(9), 1000, 1000, 600, 500)

        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(1, nyTilkjentYtelse, utbetalingTidslinje)

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
        val nyTilkjentYtelse = opprettTilkjentYtelse(startDato = start, vedtaksdato =  LocalDate.of(2025, 2, 25).plusDays(9), 1000, 0, 1000)

        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(1, nyTilkjentYtelse, utbetalingTidslinje)

        assertThat(utbetalinger.endringUtbetalinger).hasSize(1)
        assertThat(utbetalinger.endringUtbetalinger.first().perioder).hasSize(0)
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


    private fun opprettTilkjentYtelse(startDato: LocalDate, vedtaksdato: LocalDate, vararg beløpListe: Long): TilkjentYtelse {
        val perioder = beløpListe.mapIndexed { i, beløp ->
            lagTilkjentYtelsePeriode(startDato.plusWeeks(i * 2L), startDato.plusWeeks(i * 2L).plusDays(13), Beløp(beløp))
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
                utbetalingsdato = tom.plusDays(9),
            )
        )


    private fun opprettTidslinjeUtbetalinger(startDato: LocalDate, vararg beløpListe: Int): Tidslinje<UtbetalingData> {
        val segmenter = mutableListOf<Segment<UtbetalingData>>()
        val perioder = beløpListe.mapIndexed { i, beløp ->
            lagUtbetalingData(startDato.plusWeeks(i * 2L), startDato.plusWeeks(i * 2L).plusDays(13), beløp)
        }
        perioder.forEach {
            segmenter.add(Segment<UtbetalingData>(it.first, it.second))
        }
        return Tidslinje<UtbetalingData>(initSegmenter = segmenter)

    }

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