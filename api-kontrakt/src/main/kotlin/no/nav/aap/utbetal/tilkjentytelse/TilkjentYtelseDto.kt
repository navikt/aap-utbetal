package no.nav.aap.utbetal.tilkjentytelse

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilkjentYtelseDto(
    val saksnummer: String,
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val personIdent: String,
    val vedtakstidspunkt: LocalDateTime,
    val beslutterId: String,
    val saksbehandlerId: String,
    val perioder: List<TilkjentYtelsePeriodeDto>
)

data class TilkjentYtelsePeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val detaljer: TilkjentYtelseDetaljerDto
)

data class TilkjentYtelseDetaljerDto(
    val redusertDagsats: BigDecimal,
    val gradering: Int,
    val dagsats: BigDecimal,
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
    val ventedagerSamordning: Boolean = false,
    val utbetalingsdato: LocalDate
)