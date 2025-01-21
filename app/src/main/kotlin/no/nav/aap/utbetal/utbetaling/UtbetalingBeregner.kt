package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.time.LocalDateTime

class UtbetalingBeregner {

    fun tilkjentYtelseTilUtbetaling(sakUtbetalingId: Long, forrigeTilkjentYtelse: TilkjentYtelse?, nyTilkjentYtelse: TilkjentYtelse): Utbetaling {
        val forrigeTidslinje = forrigeTilkjentYtelse?.tilTidslinje() ?: Tidslinje()
        val nyTidslinje = nyTilkjentYtelse.tilTidslinje()

        val utbetalingerTidslinje = forrigeTidslinje.kombiner(nyTidslinje, prioriterHøyreSideCrossJoinMedEndring())
        val utbetalingsperioder = utbetalingerTidslinje.segmenter().map { it.verdi }
        return Utbetaling(
            sakUtbetalingId = sakUtbetalingId,
            tilkjentYtelseId = nyTilkjentYtelse.id!!,
            utbetalingOversendt = LocalDateTime.now(),
            utbetalingStatus = UtbetalingStatus.OPPRETTET,
            perioder = utbetalingsperioder,
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
                        Utbetalingsperiode(
                            periode = periode,
                            detaljer = høyre.verdi,
                            utbetalingsperiodeType = UtbetalingsperiodeType.UENDRET
                        )
                    )
                }
                return@OUTER_JOIN Segment(
                    periode,
                    Utbetalingsperiode(
                        periode = periode,
                        detaljer = høyre.verdi,
                        utbetalingsperiodeType = UtbetalingsperiodeType.ENDRET
                    )
                )
            }
            if (høyre != null) {
                return@OUTER_JOIN Segment(
                    periode,
                    Utbetalingsperiode(
                        periode = periode,
                        detaljer = høyre.verdi,
                        utbetalingsperiodeType = UtbetalingsperiodeType.NY
                    )
                )
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