package no.nav.aap.utbetal.admin

import java.time.LocalDate
import java.util.UUID

data class UtbetalingstidslinjeDto(
    val utbetalinger: List<UtbetalingDto>,
)

data class UtbetalingDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalingRef: UUID,
    val beløp: UInt,
    val fastsattDagsats: UInt,
    val utbetalingsdato: LocalDate
)
