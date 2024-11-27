package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto

class UtbetalingsplanBeregner {

    fun tilkjentYtelseTilUtbetalingsplan(forrigeTilkjentYtelse: TilkjentYtelseDto?, nyTilkjentYtelse: TilkjentYtelseDto): Utbetalingsplan {
        val forrigeTidslinje = forrigeTilkjentYtelse?.tilTidslinje() ?: Tidslinje()
        val nyTidslinje = nyTilkjentYtelse.tilTidslinje()

        val utbetalingerTidslinje = forrigeTidslinje.kombiner(nyTidslinje, prioriterHøyreSideCrossJoinMedEndring())
        val utbetalingsperioder = utbetalingerTidslinje.segmenter().map { it.verdi }
        return Utbetalingsplan(
            forrigeBehandlingsreferanse = nyTilkjentYtelse.forrigeBehandlingsreferanse,
            behandlingsreferanse = nyTilkjentYtelse.behandlingsreferanse,
            perioder = utbetalingsperioder
        )
    }

    private fun TilkjentYtelseDto.tilTidslinje() =
        Tidslinje(this.perioder.map { periode ->
            Segment<TilkjentYtelseDetaljerDto>(
                Periode(periode.fom, periode.tom),
                periode.detaljer
            )
        })

    private fun TilkjentYtelseDetaljerDto.tilUtbetaling(): Utbetaling {
        return Utbetaling(
            dagsats = Beløp(this.dagsats),
            gradering = Prosent.Companion.fraDesimal(this.gradering),
            grunnlag = Beløp(this.grunnlag),
            grunnlagsfaktor = GUnit(this.grunnlagsfaktor),
            grunnbeløp = Beløp(this.grunnbeløp),
            antallBarn = this.antallBarn,
            barnetilleggsats = Beløp(this.barnetilleggsats),
            barnetillegg = Beløp(this.barnetillegg),
        )
    }

    private fun prioriterHøyreSideCrossJoinMedEndring(): JoinStyle.OUTER_JOIN<TilkjentYtelseDetaljerDto, TilkjentYtelseDetaljerDto, Utbetalingsperiode> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (venstre != null && høyre != null) {
                if (venstre == høyre) {
                    return@OUTER_JOIN Segment(
                        periode,
                        Utbetalingsperiode.UendretPeriode(periode, høyre.verdi.tilUtbetaling())
                    )
                }
                return@OUTER_JOIN Segment(
                    periode,
                    Utbetalingsperiode.EndretPeriode(
                        periode,
                        venstre.verdi.tilUtbetaling(),
                        høyre.verdi.tilUtbetaling()
                    )
                )
            }
            if (høyre != null) {
                return@OUTER_JOIN Segment(periode, Utbetalingsperiode.NyPeriode(periode, høyre.verdi.tilUtbetaling()))
            } else {
                if (venstre == null)  {
                    return@OUTER_JOIN null
                } else {
                    throw IllegalStateException("Periode i forrige behandling finnes ikke i ny behandling: $periode")
                }
            }
        }
    }

}