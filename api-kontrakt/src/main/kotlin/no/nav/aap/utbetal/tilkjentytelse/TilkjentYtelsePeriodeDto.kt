package no.nav.aap.utbetal.tilkjentytelse

import java.time.LocalDate

data class TilkjentYtelsePeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val detaljer: TilkjentYtelseDetaljerDto
)