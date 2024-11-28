package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljer

class UtbetalingsplanBeregner {

    fun tilkjentYtelseTilUtbetalingsplan(forrigeTilkjentYtelse: TilkjentYtelse?, nyTilkjentYtelse: TilkjentYtelse): Utbetalingsplan {
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

    private fun TilkjentYtelse.tilTidslinje() =
        Tidslinje(this.perioder.map { periode ->
            Segment<TilkjentYtelseDetaljer>(
                periode.periode,
                periode.detaljer
            )
        })

    private fun TilkjentYtelseDetaljer.tilUtbetaling(): Utbetaling {
        return Utbetaling(
            redusertDagsats = this.redusertDagsats,
            dagsats = this.dagsats,
            gradering = this.gradering,
            grunnlag = this.grunnlag,
            grunnlagsfaktor = this.grunnlagsfaktor,
            grunnbeløp = this.grunnbeløp,
            antallBarn = this.antallBarn,
            barnetilleggsats = this.barnetilleggsats,
            barnetillegg = this.barnetillegg,
        )
    }

    private fun prioriterHøyreSideCrossJoinMedEndring(): JoinStyle.OUTER_JOIN<TilkjentYtelseDetaljer, TilkjentYtelseDetaljer, Utbetalingsperiode> {
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