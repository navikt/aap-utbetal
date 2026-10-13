package no.nav.aap.utbetal.simuleringv2

import java.time.LocalDate

sealed class SimuleringsresponseDto

data class SimuleringDto(
    val perioder: List<SimuleringsperiodeDto>
) : SimuleringsresponseDto()

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
    val posteringer: List<PosteringDto>,
)

data class PosteringDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val type: String,
    val klassekode: String,
)

enum class SimuleringStatus {
    OK_UTEN_ENDRING,
    FEILET,
    UTILGJENGELIG,
    UGYLDIG_REQUEST,
}

data class InfoDto(
    val status: SimuleringStatus,
    val fagsystem: String,
    val message: String,
): SimuleringsresponseDto()
