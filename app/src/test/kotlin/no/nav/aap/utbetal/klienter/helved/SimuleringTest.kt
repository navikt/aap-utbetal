package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test


class SimuleringTest {

    @Test
    fun `Skal kunne klippe opp simulering med ingen perioder`() {
        val simulering = Simulering(perioder = listOf())
        val klippetSimulering = simulering.klipp(listOf())
        assertThat(klippetSimulering.perioder).hasSize(0)
    }

    @Test
    fun `Skal kunne klippe opp simulering med en periode`() {
        val simulering = Simulering(perioder = listOf(
            simPeriode("2025-01-01/2025-01-10")
        ))
        val klippetSimulering = simulering.klipp(listOf())
        assertThat(klippetSimulering.perioder).hasSize(1)
    }

    @Test
    fun `Skal kunne klippe opp simulering med en periode med en klippeperiode`() {
        val simulering = Simulering(perioder = listOf(
            simPeriode("2025-01-01/2025-01-10")
        ))
        val klippetSimulering = simulering.klipp(listOf(periode("2025-01-01/2025-01-05")))
        assertThat(klippetSimulering.perioder).hasSize(1)
        assertThat(klippetSimulering.perioder[0].fom).isEqualTo(LocalDate.parse("2025-01-06"))
        assertThat(klippetSimulering.perioder[0].tom).isEqualTo(LocalDate.parse("2025-01-10"))
    }

    @Test
    fun `Skal kunne klippe opp simulering med en periode med en klippeperiode midt i simulering`() {
        val simulering = Simulering(perioder = listOf(
            simPeriode("2025-01-01/2025-01-20")
        ))
        val klippetSimulering = simulering.klipp(listOf(periode("2025-01-07/2025-01-14")))
        assertThat(klippetSimulering.perioder).hasSize(2)
        assertThat(klippetSimulering.perioder[0].fom).isEqualTo(LocalDate.parse("2025-01-01"))
        assertThat(klippetSimulering.perioder[0].tom).isEqualTo(LocalDate.parse("2025-01-06"))
        assertThat(klippetSimulering.perioder[1].fom).isEqualTo(LocalDate.parse("2025-01-15"))
        assertThat(klippetSimulering.perioder[1].tom).isEqualTo(LocalDate.parse("2025-01-20"))
    }

    private fun simPeriode(periode: String): Simuleringsperiode {
        val p = periode(periode)
        return Simuleringsperiode(
            fom = p.fom,
            tom = p.tom,
            utbetalinger = listOf(SimulertUtbetaling(
                sakId = "1",
                utbetalesTil = "2",
                tidligereUtbetalt = 1000,
                nyttBeløp = 800
            ))
        )
    }

    private fun periode(periode: String): Periode {
        val (fom, tom) = periode.split("/").let {LocalDate.parse(it[0]) to LocalDate.parse(it[1]) }
        return Periode(fom, tom)
    }

}