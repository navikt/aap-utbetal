package no.nav.aap.utbetaling.tilkjentytelse

import java.time.LocalDate

data class TilkjentYtelsePeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int
)