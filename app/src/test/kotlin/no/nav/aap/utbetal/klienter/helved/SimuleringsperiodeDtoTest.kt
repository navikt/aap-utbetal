package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_STØNADSPERIODE_KODE
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.utbetal.simuleringv2.InfoDto
import no.nav.aap.utbetal.simuleringv2.PosteringDto
import no.nav.aap.utbetal.simuleringv2.SimuleringDto
import no.nav.aap.utbetal.simuleringv2.SimuleringStatus
import no.nav.aap.utbetal.simuleringv2.SimuleringsperiodeDto
import no.nav.aap.utbetal.simuleringv2.SimuleringsresponseDto
import no.nav.aap.utbetal.simuleringv2.SimulertUtbetalingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * NB: Testen ligger i denne modulen for å kunne bruke DefaultJsonMapper som ikke er tilgjengelig i api-kontrakt.
 */
class SimuleringsperiodeDtoTest {

    @Test
    fun `InfoDto til json og tilbake`() {
        val infoDto = InfoDto(
            status = SimuleringStatus.OK_UTEN_ENDRING,
            fagsystem = "AAP",
            message = "testing"
        )

        val infoDtoJson = DefaultJsonMapper.toJson(infoDto)
        val deserializedInfoDto = DefaultJsonMapper.fromJson<SimuleringsresponseDto>(infoDtoJson)

        assertThat(deserializedInfoDto).isEqualTo(infoDto)
    }

    @Test
    fun `SimuleringDto til json og tilbake`() {
        val simuleringDto = SimuleringDto(
            perioder = listOf(
                SimuleringsperiodeDto(
                    fom = LocalDate.parse("2026-11-13"),
                    tom = LocalDate.parse("2026-11-13"),
                    utbetalinger = listOf(
                        SimulertUtbetalingDto(
                            fagsystem = "AAP",
                            sakId = "ABC123",
                            utbetalesTil = "fnr",
                            stønadstype = AVKLAR_STØNADSPERIODE_KODE,
                            tidligereUtbetalt = 1000,
                            nyttBeløp = 1200,
                            posteringer = listOf(
                                PosteringDto(
                                    fom = LocalDate.parse("2026-11-13"),
                                    tom = LocalDate.parse("2026-11-13"),
                                    beløp = 200,
                                    type = "type",
                                    klassekode = "klassekode"
                                )
                            )
                        )
                    )
                )
            )
        )

        val simuleringDtoJson = DefaultJsonMapper.toJson(simuleringDto)
        val deserializedSimuleringDto = DefaultJsonMapper.fromJson<SimuleringsresponseDto>(simuleringDtoJson)

        assertThat(deserializedSimuleringDto).isEqualTo(simuleringDto)
    }

}