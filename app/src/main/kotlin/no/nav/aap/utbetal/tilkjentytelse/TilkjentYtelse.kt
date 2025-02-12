package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.felles.YtelseDetaljer
import java.util.UUID

data class TilkjentYtelse(
    val id: Long? = null,
    val saksnummer: Saksnummer,
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val personIdent: String,
    val perioder: List<TilkjentYtelsePeriode>
)

data class TilkjentYtelsePeriode(
    val periode: Periode,
    val detaljer: YtelseDetaljer
)

