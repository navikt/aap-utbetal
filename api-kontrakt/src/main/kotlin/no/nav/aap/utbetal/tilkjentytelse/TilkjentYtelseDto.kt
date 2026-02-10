package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.utbetal.kodeverk.AvventÅrsak
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
    val perioder: List<TilkjentYtelsePeriodeDto>,
    val avvent: TilkjentYtelseAvventDto? = null,
    val nyMeldeperiode: MeldeperiodeDto? = null,
    val trekk: List<TilkjentYtelseTrekkDto> = emptyList(),
)

data class MeldeperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
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
    val utbetalingsdato: LocalDate,
    val meldeperiodeDto: MeldeperiodeDto? = null,
)

data class TilkjentYtelseAvventDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val overføres: LocalDate?,
    val årsak: AvventÅrsak? = null,
    val feilregistrering: Boolean = false,
)

data class TilkjentYtelseTrekkDto(
    val dato: LocalDate,
    val beløp: Int,
)