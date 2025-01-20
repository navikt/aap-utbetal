package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetaling.UtbetalingsperiodeType

data class NesteUtbetaling(
    val nesteUtbetaling: Periode,
    val utbetalingsperioderSomErEndretFørNesteUtbetaling: List<Utbetalingsperiode>,
    val utbetalingsperioderNesteUtbetaling: List<Utbetalingsperiode>
)

class UtbetalingBeregner {
    fun beregnUtbetaling(utbetaling: Utbetaling, nesteUtbetaling: Periode): NesteUtbetaling {
        val perioderSomErEndretFørNesteUtbetaling = utbetaling.perioder
            .filter { it.utbetalingsperiodeType == UtbetalingsperiodeType.ENDRET && it.periode.tom < nesteUtbetaling.fom }
        val utbetalingsperioderNesteUtbetaling = Tidslinje(utbetaling.perioder.map { Segment<Utbetalingsperiode>(it.periode, it) })
            // Klipp ut perioder ut perioden det skal utbetales for
            .disjoint(nesteUtbetaling)
            // Oppdater periodene med i Utbetalingsperiode objektene
            .map {
                it.verdi.copy(
                    periode = Periode(it.periode.fom, it.periode.tom)
                )
            }
        return NesteUtbetaling(
            nesteUtbetaling,
            perioderSomErEndretFørNesteUtbetaling,
            utbetalingsperioderNesteUtbetaling
        )
    }


}