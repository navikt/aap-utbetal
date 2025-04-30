package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.utbetal.simulering.SimuleringDto
import no.nav.aap.utbetal.simulering.SimuleringsperiodeDto
import no.nav.aap.utbetal.simulering.SimulertUtbetalingDto
import java.time.LocalDate

data class Simulering(
    val perioder: List<Simuleringsperiode>
) {
    fun tilSimuleringDto() = SimuleringDto(
        perioder = perioder.map { it.tilSimuleringsperiode() }
    )
}

data class Simuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<SimulertUtbetaling>
) {
    fun tilSimuleringsperiode() = SimuleringsperiodeDto(
        fom = fom,
        tom = tom,
        utbetalinger = utbetalinger.map { it.tilSimulertUtbetalingDto() }
    )
}

data class SimulertUtbetaling(
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