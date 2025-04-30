package no.nav.aap.utbetal.simulering

import java.time.LocalDate

data class SimuleringDto(
    val perioder: List<SimuleringsperiodeDto>
)

data class SimuleringsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<SimulertUtbetalingDto>
)

data class SimulertUtbetalingDto(
    val fagsystem: String,
    val sakId: String,
    val utbetalesTil: String,
    val stønadstype: String,
    val tidligereUtbetalt: Int,
    val nyttBeløp: Int,
)