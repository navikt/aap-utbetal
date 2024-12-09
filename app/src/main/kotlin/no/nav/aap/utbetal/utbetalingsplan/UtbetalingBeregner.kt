package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

data class NesteUtbetaling(
    val nesteUtbetaling: Periode,
    val utbetalingsperioderSomErEndretFørNesteUtbetaling: List<Utbetalingsperiode>,
    val utbetalingsperioderNesteUtbetaling: List<Utbetalingsperiode>
)

class UtbetalingBeregner {
    fun beregnUtbetaling(utbetalingsplan: Utbetalingsplan, nesteUtbetaling: Periode): NesteUtbetaling {
        val perioderSomErEndretFørNesteUtbetaling = utbetalingsplan.perioder
            .filter { it.utbetalingsperiodeType == UtbetalingsperiodeType.ENDRET && it.periode.tom < nesteUtbetaling.fom }
        val utbetalingsperioderNesteUtbetaling = Tidslinje(utbetalingsplan.perioder.map { Segment<Utbetalingsperiode>(it.periode, it) })
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