package no.nav.aap.utbetal.tilkjentytelse

import java.util.UUID

data class TilkjentYtelseDto(
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val perioder: List<TilkjentYtelsePeriodeDto>
)

