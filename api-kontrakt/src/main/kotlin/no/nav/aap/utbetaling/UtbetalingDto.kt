package no.nav.aap.utbetaling

import java.math.BigDecimal
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
    BEKREFTET,
    FEILET
}

data class UtbetalingsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val redusertDagsats: BigDecimal,
    val dagsats: BigDecimal,
    val gradering: Int,
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
    val utbetalingsperiodeType: UtbetalingsperiodeType,
    val ventedagerSamordning: Boolean
)

enum class UtbetalingsperiodeType {
    NY,
    ENDRET,
    UENDRET
}

