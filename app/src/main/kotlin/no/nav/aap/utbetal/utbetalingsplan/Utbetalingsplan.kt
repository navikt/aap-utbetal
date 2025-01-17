package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.felles.YtelseDetaljer
import java.util.UUID

data class Utbetalingsplan(
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val sakUtbetalingId: Long,
    val tilkjentYtelseId: Long,
    val perioder: List<Utbetalingsperiode>
)

enum class UtbetalingsperiodeType {
    NY,
    ENDRET,
    UENDRET
}

data class Utbetalingsperiode(val periode: Periode, val detaljer: YtelseDetaljer, val utbetalingsperiodeType: UtbetalingsperiodeType)

