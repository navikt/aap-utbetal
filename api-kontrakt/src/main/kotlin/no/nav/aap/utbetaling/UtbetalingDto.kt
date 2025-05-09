package no.nav.aap.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime

data class UtbetalingDto(
    val utbetalingOversendt: LocalDateTime,
    val utbetalingBekreftet: LocalDateTime? = null,
    val utbetalingStatus: UtbetalingStatus,
    val perioder: List<UtbetalingsperiodeDto>
)

enum class UtbetalingStatus {
    OPPRETTET,
    SENDT,
    BEKREFTET,
    FEILET,
    INGEN_PERIODER
}

data class UtbetalingsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val fastsattDagsats: Int,
    val utbetalingsperiodeType: UtbetalingsperiodeType,
    val utbetalingsdato: LocalDate
)

enum class UtbetalingsperiodeType {
    NY,
    ENDRET,
    UENDRET
}

