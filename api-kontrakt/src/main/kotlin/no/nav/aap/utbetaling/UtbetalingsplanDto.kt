package no.nav.aap.utbetaling

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class UtbetalingsplanDto(
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val perioder: List<UtbetalingsperiodeDto>
)

data class UtbetalingsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: BigDecimal,
    val gradering: Int,
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
    val endretSidenForrrige: Boolean = false,
)

