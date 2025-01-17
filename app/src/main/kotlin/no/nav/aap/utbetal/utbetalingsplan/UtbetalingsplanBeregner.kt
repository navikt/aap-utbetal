package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse

class UtbetalingsplanBeregner {

    fun tilkjentYtelseTilUtbetalingsplan(sakUtbetalingId: Long, forrigeTilkjentYtelse: TilkjentYtelse?, nyTilkjentYtelse: TilkjentYtelse): Utbetalingsplan {
        val forrigeTidslinje = forrigeTilkjentYtelse?.tilTidslinje() ?: Tidslinje()
        val nyTidslinje = nyTilkjentYtelse.tilTidslinje()

        val utbetalingerTidslinje = forrigeTidslinje.kombiner(nyTidslinje, prioriterHøyreSideCrossJoinMedEndring())
        val utbetalingsperioder = utbetalingerTidslinje.segmenter().map { it.verdi }
        return Utbetalingsplan(
            forrigeBehandlingsreferanse = nyTilkjentYtelse.forrigeBehandlingsreferanse,
            behandlingsreferanse = nyTilkjentYtelse.behandlingsreferanse,
            perioder = utbetalingsperioder,
            tilkjentYtelseId = nyTilkjentYtelse.id!!,
            sakUtbetalingId = sakUtbetalingId
        )
    }

    private fun TilkjentYtelse.tilTidslinje() =
        Tidslinje(this.perioder.map { periode ->
            Segment<YtelseDetaljer>(
                periode.periode,
                periode.detaljer
            )
        })

    private fun prioriterHøyreSideCrossJoinMedEndring(): JoinStyle.OUTER_JOIN<YtelseDetaljer, YtelseDetaljer, Utbetalingsperiode> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (venstre != null && høyre != null) {
                if (venstre == høyre) {
                    return@OUTER_JOIN Segment(
                        periode,
                        Utbetalingsperiode(periode, høyre.verdi, UtbetalingsperiodeType.UENDRET)
                    )
                }
                return@OUTER_JOIN Segment(
                    periode,
                    Utbetalingsperiode(periode, høyre.verdi, UtbetalingsperiodeType.ENDRET)
                )
            }
            if (høyre != null) {
                return@OUTER_JOIN Segment(periode, Utbetalingsperiode(periode, høyre.verdi, UtbetalingsperiodeType.NY))
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