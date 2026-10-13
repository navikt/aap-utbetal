package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.simulering.SimuleringDto
import no.nav.aap.utbetal.simulering.SimuleringsperiodeDto
import no.nav.aap.utbetal.simulering.SimulertUtbetalingDto
import java.time.LocalDate

data class SimuleringV1(
    val perioder: List<SimuleringsperiodeV1>
) {
    fun tilSimuleringDto() = SimuleringDto(
        perioder = perioder.map { it.tilSimuleringsperiode() }
    )

    fun klipp(perioder: List<Periode>): SimuleringV1 {
        val klippePerioder =
            Tidslinje(perioder.map { Segment(it, Unit) })
        val simuleringsperioderTidslinje =
            Tidslinje(this.perioder.map { periode -> Segment(Periode(periode.fom, periode.tom), periode.utbetalinger) })
        val klippetTidslinje = simuleringsperioderTidslinje.disjoint(klippePerioder, { p, v -> Segment(p, v.verdi)})
        return SimuleringV1(
            perioder =  klippetTidslinje
                .segmenter()
                .map { segment -> SimuleringsperiodeV1(fom = segment.fom(), tom = segment.tom(), utbetalinger = segment.verdi) }
        )
    }
}

data class SimuleringsperiodeV1(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<SimulertUtbetalingV1>
) {
    fun tilSimuleringsperiode() = SimuleringsperiodeDto(
        fom = fom,
        tom = tom,
        utbetalinger = utbetalinger.map { it.tilSimulertUtbetalingDto() }
    )
}

data class SimulertUtbetalingV1(
    val fagsystem: String = "AAP",
    val sakId: String,
    val utbetalesTil: String,
    val stønadstype: String = "AAP_UNDER_ARBEIDSAVKLARING",
    val tidligereUtbetalt: Int,
    val nyttBeløp: Int,
) {
    fun tilSimulertUtbetalingDto() = SimulertUtbetalingDto(
        fagsystem = fagsystem,
        sakId = sakId,
        utbetalesTil = utbetalesTil,
        stønadstype = stønadstype,
        tidligereUtbetalt = tidligereUtbetalt,
        nyttBeløp = nyttBeløp

    )
}