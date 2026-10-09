package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.utbetal.simuleringv2.SimuleringStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SimuleringsresponsTest {

    @Test
    fun `kan mappe info-melding`() {
        val json = """
            {
              "@type": "info",
              "status": "OK_UTEN_ENDRING",
              "fagsystem": "AAP",
              "message": "Simulering er vellykket, men det er ingen posteringer å vise til.Det er to mulige årsaker til dette. 1) Oppdraget finnes fra før, det er ingen endring mellom det som simuleres og det som eksisterer fra før. 2) Det simuleres et opphør på et oppdrag som ikke er ajourholdt, hvis oppdraget iverksettes så vil opphøret skje uten at noe utbetales."
            }
        """.trimIndent()

        val simuleringsrespons = DefaultJsonMapper.fromJson<Simuleringsrespons>(json)

        assertThat(simuleringsrespons).isInstanceOf(Info::class.java)
        val infoMelding = simuleringsrespons as Info
        assertThat(infoMelding.status).isEqualTo(SimuleringStatus.OK_UTEN_ENDRING)
        assertThat(infoMelding.fagsystem).isEqualTo("AAP")
    }

    @Test
    fun `kan mappe simulering-melding`() {
        val json = """
            {
              "@type": "v2",
              "perioder": [
                {
                    "fom": "2026-10-05",
                    "tom": "2026-10-09",
                    "utbetalinger": [
                        {
                            "fagsystem": "AAP",
                            "sakId": "5jdg68",
                            "utbetalesTil": "01010112345",
                            "stønadstype": "AAP_UNDER_ARBEIDSAVKLARING",
                            "tidligereUtbetalt": 1000,
                            "nyttBeløp": 1200,
                            "posteringer": [
                                {
                                    "fom": "2026-10-05",
                                    "tom": "2026-10-09",
                                    "beløp": 200,
                                    "type": "YTEL",
                                    "klassekode": "AAPOR"
                                }
                            ]
                        }
                    ]
                }
              ]
            }
        """.trimIndent()

        val simuleringsrespons = DefaultJsonMapper.fromJson<Simuleringsrespons>(json)

        assertThat(simuleringsrespons).isInstanceOf(Simulering::class.java)
        val simulering = simuleringsrespons as Simulering
        assertThat(simulering.perioder).hasSize(1)
        assertThat(simulering.perioder.first().utbetalinger).hasSize(1)
        assertThat(simulering.perioder.first().utbetalinger.first().posteringer).hasSize(1)
    }

}