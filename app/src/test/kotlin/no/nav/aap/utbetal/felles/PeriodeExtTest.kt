package no.nav.aap.utbetal.felles

import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class PeriodeExtTest {

    @Test
    fun `Periode uten helg`() {
        val periode = Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 14)) //Mandag til fredag
        assertThat(periode.finnHelger()).isEmpty()
    }

    @Test
    fun `Periode med bare helg`() {
        val periode = Periode(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 2, 16)) //Lørdag til søndag

        val helger = periode.finnHelger()
        assertThat(helger).hasSize(1)
        assertThat(helger.first()).isEqualTo(periode)
    }

    @Test
    fun `Periode over en helg`() {
        val periode = Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 20)) //Lørdag til søndag

        val helger = periode.finnHelger()
        assertThat(helger).hasSize(1)
        assertThat(helger.first()).isEqualTo(Periode(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 2, 16)))
    }

    @Test
    fun `Periode over to helger`() {
        val periode = Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 24)) //Lørdag til søndag

        val helger = periode.finnHelger()
        assertThat(helger).hasSize(2)
        assertThat(helger).isEqualTo(listOf(
            Periode(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 2, 16)),
            Periode(LocalDate.of(2025, 2, 22), LocalDate.of(2025, 2, 23))
        ))
    }

    @Test
    fun `Periode som begynner på en søndag`() {
        val periode = Periode(LocalDate.of(2025, 2, 16), LocalDate.of(2025, 2, 20))

        val helger = periode.finnHelger()
        assertThat(helger).hasSize(1)
        assertThat(helger.first()).isEqualTo(Periode(LocalDate.of(2025, 2, 16), LocalDate.of(2025, 2, 16)))
    }

    @Test
    fun `Periode som slutter på en lørdag`() {
        val periode = Periode(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 15))

        val helger = periode.finnHelger()
        assertThat(helger).hasSize(1)
        assertThat(helger.first()).isEqualTo(Periode(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 2, 15)))
    }

}