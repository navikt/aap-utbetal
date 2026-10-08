package no.nav.aap.utbetal.klienter.helved

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.utbetal.simuleringv2.InfoDto
import no.nav.aap.utbetal.simuleringv2.PosteringDto
import no.nav.aap.utbetal.simuleringv2.SimuleringDto
import no.nav.aap.utbetal.simuleringv2.SimuleringStatus
import no.nav.aap.utbetal.simuleringv2.SimuleringsperiodeDto
import no.nav.aap.utbetal.simuleringv2.SimuleringsresponseDto
import no.nav.aap.utbetal.simuleringv2.SimulertUtbetalingDto
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = Simulering::class, name = "v2"),
        JsonSubTypes.Type(value = Info::class, name = "info")
    ]
)
sealed class Simuleringsrespons {
    abstract fun tilDto(): SimuleringsresponseDto
}

@JsonTypeName("v2")
data class Simulering(
    val perioder: List<Simuleringsperiode>
): Simuleringsrespons() {
    override fun tilDto() = SimuleringDto(
        perioder = perioder.map { it.tilSimuleringsperiode() }
    )
}

@JsonTypeName("info")
data class Info(
    val status: SimuleringStatus,
    val fagsystem: String,
    val message: String,
): Simuleringsrespons() {
    override fun tilDto() = InfoDto(
        status = this.status,
        fagsystem = this.fagsystem,
        message = this.message,
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
        utbetalinger = utbetalinger.map { it.tilDto() }
    )
}

data class SimulertUtbetaling(
    val fagsystem: String = "AAP",
    val sakId: String,
    val utbetalesTil: String,
    val stønadstype: String = "AAP_UNDER_ARBEIDSAVKLARING",
    val tidligereUtbetalt: Int,
    val nyttBeløp: Int,
    val posteringer: List<Postering>,
) {
    fun tilDto() = SimulertUtbetalingDto(
        fagsystem = fagsystem,
        sakId = sakId,
        utbetalesTil = utbetalesTil,
        stønadstype = stønadstype,
        tidligereUtbetalt = tidligereUtbetalt,
        nyttBeløp = nyttBeløp,
        posteringer = posteringer.map { it.tilDto() }
    )
}

data class Postering(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val type: String,
    val klassekode: String,
) {
    fun tilDto() = PosteringDto(
        fom = this.fom,
        tom = this.tom,
        beløp = this.beløp,
        type = this.type,
        klassekode = this.klassekode,
    )
}
