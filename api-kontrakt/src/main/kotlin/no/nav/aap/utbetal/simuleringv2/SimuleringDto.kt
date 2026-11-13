package no.nav.aap.utbetal.simuleringv2

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class SimuleringsresponseDto

@JsonTypeName("simulering")
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

@JsonTypeName("info")
data class InfoDto(
    val status: SimuleringStatus,
    val fagsystem: String,
    val message: String,
): SimuleringsresponseDto()
