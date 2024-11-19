package no.nav.aap.utbetal.tilkjentytelse

import java.math.BigDecimal

data class TilkjentYtelseDetaljerDto(
    val gradering: BigDecimal,
    val dagsats: BigDecimal,
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal
)