package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.time.LocalDateTime

data class Utbetaling(
    val id: Long? = null,
    val sakUtbetalingId: Long,
    val tilkjentYtelseId: Long,
    val utbetalingOversendt: LocalDateTime,
    val utbetalingBekreftet: LocalDateTime? = null,
    val utbetalingStatus: UtbetalingStatus,
    val perioder: List<Utbetalingsperiode>
)

data class Utbetalingsperiode(
    val id: Long? = null,
    val periode: Periode,
    val detaljer: YtelseDetaljer,
    val utbetalingsperiodeType: UtbetalingsperiodeType
)

