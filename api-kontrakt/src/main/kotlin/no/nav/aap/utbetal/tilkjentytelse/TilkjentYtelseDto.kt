package no.nav.aap.utbetal.tilkjentytelse

import java.util.UUID

data class TilkjentYtelseDto(
    val saksnummer: String,
    val behandlingsreferanse: UUID,
    val perioder: List<TilkjentYtelsePeriodeDto>
)

